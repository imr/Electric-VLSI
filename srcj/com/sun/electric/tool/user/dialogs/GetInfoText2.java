/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: GetInfoText2.java
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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import javax.swing.JFrame;

/**
* Class to handle the "Text Get-Info" dialog.
*/
public class GetInfoText2 extends EDialog {
    private static GetInfoText2 theDialog = null;
    private Highlight shownText;
    private String initialText;

    private Variable var;
    private TextDescriptor td;
    private ElectricObject owner;

    /**
     * Method to show the Text Get-Info dialog.
     */
    public static void showDialog() {
       if (theDialog == null) {
           if (TopLevel.isMDIMode()) {
               JFrame jf = TopLevel.getCurrentJFrame();
               theDialog = new GetInfoText2(jf, false);
           } else {
               theDialog = new GetInfoText2(null, false);
           }
       }
       theDialog.show();
    }

   /**
    * Method to reload the Text Get-Info dialog from the current highlighting.
    */
   public static void load() {
       if (theDialog == null) return;
       theDialog.loadTextInfo();
   }

   private void loadTextInfo() {
       // must have a single text selected
       Highlight textHighlight = null;
       int textCount = 0;
       for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
           Highlight h = (Highlight) it.next();
           if (h.getType() != Highlight.Type.TEXT) continue;
            // ignore export text
           if (h.getVar() == null && h.getElectricObject() instanceof Export) continue;
           textHighlight = h;
           textCount++;
       }
        if (textCount > 1) textHighlight = null;
       boolean enabled = (textHighlight == null) ? false : true;
       // enable or disable everything
       for (int i = 0; i < getComponentCount(); i++) {
           Component c = getComponent(i);
           c.setEnabled(enabled);
       }
        if (!enabled) {
           header.setText("No Text Selected");
           evaluation.setText("");
           theText.setText("");
           shownText = null;
           textPanel.setTextDescriptor(null, null, null);
           attrPanel.setVariable(null, null, null, null);
           return;
       }
        String description = "Unknown text";
       initialText = "";
       td = null;
       owner = textHighlight.getElectricObject();
       NodeInst ni = null;
       if (owner instanceof NodeInst) ni = (NodeInst) owner;
       var = textHighlight.getVar();
       if (var != null) {
            td = var.getTextDescriptor();
           Object obj = var.getObject();
           if (obj instanceof Object[]) {
               // unwind the array elements by hand
               Object[] theArray = (Object[]) obj;
               initialText = "";
               for (int i = 0; i < theArray.length; i++) {
                   if (i != 0) initialText += "\n";
                   initialText += theArray[i];
               }
           } else {
               initialText = var.getPureValue(-1, -1);
           }
           description = var.getFullDescription(owner);
       } else {
            if (textHighlight.getName() != null) {
               if (owner instanceof Geometric) {
                   Geometric geom = (Geometric) owner;
                   td = geom.getNameTextDescriptor();
                   if (geom instanceof NodeInst) {
                       description = "Name of node " + ((NodeInst) geom).getProto().describe();
                   } else {
                       description = "Name of arc " + ((ArcInst) geom).getProto().describe();
                   }
                   initialText = geom.getName();
               }
           } else if (owner instanceof NodeInst) {
                description = "Name of cell instance " + ni.describe();
               td = ni.getProtoTextDescriptor();
               initialText = ni.getProto().describe();
           }
       }
        header.setText(description);
       theText.setText(initialText);
       theText.setEditable(true);
        // if the var is code, evaluate it
       evaluation.setText("");
       if (var != null) {
           if (var.isCode()) {
               evaluation.setText("Evaluation: " + var.describe(-1, -1));
           }
       }
        // set the text edit panel
       textPanel.setTextDescriptor(td, null, owner);
       attrPanel.setVariable(var, td, null, owner);
        shownText = textHighlight;
    }

    /**
     * Creates new form Text Get-Info
     */
    private GetInfoText2(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        getRootPane().setDefaultButton(ok);

       loadTextInfo();
    }

    protected static class ChangeText extends Job {
        Variable var;
        Name name;
        ElectricObject owner;
        String[] newText;

       protected ChangeText(Variable var, Name name, ElectricObject owner, String[] newText) {
            super("Modify Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.var = var;
            this.name = name;
            this.owner = owner;
            this.newText = newText;
            startJob();
        }

       public void doIt() {
            if (var != null) {
                Variable newVar = null;
                if (newText.length > 1) {
                    newVar = owner.updateVar(var.getKey(), newText);
                } else {
                    // change variable
                    newVar = owner.updateVar(var.getKey(), newText[0]);
                }
            } else {
                if (name != null) {
                    if (owner != null) {
                        // change name of NodeInst or ArcInst
                        ((Geometric) owner).setName(newText[0]);
                    }
                }
            }
            GetInfoText2.load();
        }
    }

    /**
     * Job to trigger update to Attributes dialog.  Type set to CHANGE and priority to USER
     * <p/>
     * so that in queues in order behind other Jobs from this class: this assures it will
     * <p/>
     * occur after the queued changes
     */
     private static class UpdateDialog extends Job {
        private UpdateDialog() {
            super("Update Attributes Dialog", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }
        public void doIt() {
            GetInfoText2.load();
        }
    }

    /**
     * This method is called from within the constructor to
     * <p/>
     * initialize the form.
     */
     private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

       cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        header = new javax.swing.JLabel();
        apply = new javax.swing.JButton();
        evaluation = new javax.swing.JLabel();
        theText = new javax.swing.JTextArea();
        textPanel = new TextInfoPanel();
        attrPanel = new TextAttributesPanel();
        buttonsPanel = new javax.swing.JPanel();

       getContentPane().setLayout(new java.awt.GridBagLayout());

       setTitle("Text Information");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

       header.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        getContentPane().add(header, gridBagConstraints);

       gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(theText, gridBagConstraints);

       evaluation.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(evaluation, gridBagConstraints);

       gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(textPanel, gridBagConstraints);

       gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(attrPanel, gridBagConstraints);

       cancel.setText("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

       apply.setText("Apply");
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                applyActionPerformed(evt);
            }
        });

       gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

       ok.setText("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

       gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);
/*

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(buttonsPanel, gridBagConstraints);
*/

        pack();
    }

    private void applyActionPerformed(ActionEvent evt) {
        if (shownText == null) return;

        // tell sub-panels to update if they have changed
        textPanel.applyChanges();
        attrPanel.applyChanges();

       boolean changed = false;

        // see if text changed
        String currentText = theText.getText();
        if (!currentText.equals(initialText)) changed = true;

       if (changed) {
            // split text into lines
            String[] textArray = new String[theText.getLineCount()];
            for (int i = 0; i < theText.getLineCount(); i++) {
                try {
                    int startPos = theText.getLineStartOffset(i);
                    int endPos = theText.getLineEndOffset(i);
                    if (currentText.charAt(endPos - 1) == '\n') endPos--;
                    textArray[i] = currentText.substring(startPos, endPos);
                } catch (javax.swing.text.BadLocationException e) {
                }
            }

           if (textArray.length > 0) {
                // generate job to change text
                ChangeText job = new ChangeText(var, shownText.getName(), owner, textArray);
                initialText = currentText;
            }
        }
        // update dialog
        UpdateDialog job2 = new UpdateDialog();

    }

    private void okActionPerformed(ActionEvent evt) {
        applyActionPerformed(evt);
        closeDialog(null);
    }

    private void cancelActionPerformed(ActionEvent evt) {
        closeDialog(null);
    }

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        setVisible(false);
        //theDialog = null;
        //dispose();
    }

    private javax.swing.JButton apply;
    private javax.swing.JButton cancel;
    private javax.swing.JLabel evaluation;
    private javax.swing.JLabel header;
    private javax.swing.JButton ok;
    private javax.swing.JTextArea theText;
    private javax.swing.JPanel buttonsPanel;
    private TextInfoPanel textPanel;
    private TextAttributesPanel attrPanel;
}
