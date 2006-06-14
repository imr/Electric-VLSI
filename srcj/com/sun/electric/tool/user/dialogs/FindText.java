/* -*- tab-width: 4 -*-i
 *
 * Electric(tm) VLSI Design System
 *
 * File: FindText.java
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

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;


/**
 * Class to handle the "Search and Replace" dialog.
 */
public class FindText extends EDialog
{
    private static Pref.Group prefs = Pref.groupForPackage(FindText.class);
	private static Pref
		prefCaseSensitive = Pref.makeBooleanPref("FindText_caseSensitive", prefs, false),
		prefFindTextMessage = Pref.makeStringPref("FindText_findTextMessage", prefs, ""),
		prefReplaceTextMessage = Pref.makeStringPref("FindText_ReplaceTextMessage", prefs, ""),
	    prefFindReverse = Pref.makeBooleanPref("FindText_findReverse", prefs, false),
	    prefRegExp = Pref.makeBooleanPref("FindText_regExp", prefs, false),
	    prefSearchNodeNames = Pref.makeBooleanPref("FindText_searchNodeNames", prefs, true),
	    prefSearchNodeVars = Pref.makeBooleanPref("FindText_searchNodeVars", prefs, true),
	    prefSearchArcNames = Pref.makeBooleanPref("FindText_searchArcNames", prefs, true),
	    prefSearchArcVars = Pref.makeBooleanPref("FindText_searchArcVars", prefs, true),
	    prefSearchExportNames = Pref.makeBooleanPref("FindText_searchExportNames", prefs, true),
	    prefSearchExportVars = Pref.makeBooleanPref("FindText_searchExportVars", prefs, true),
	    prefSearchCellVars = Pref.makeBooleanPref("FindText_searchCellVars", prefs, true),
	    prefSearchTempNames = Pref.makeBooleanPref("FindText_searchTempNames", prefs, false);
	private String lastSearch = null;

	public static void findTextDialog()
	{
		FindText dialog = new FindText(TopLevel.getCurrentJFrame(), false);
		dialog.setVisible(true);
	}

    // Method to clean last search
    private void cleanLastSearch()
    {
        lastSearch = null;
    }

	/** Creates new form Search and Replace */
	private FindText(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		findString.setText(prefFindTextMessage.getString());
		replaceString.setText(prefReplaceTextMessage.getString());
		caseSensitive.setSelected(prefCaseSensitive.getBoolean());
		findReverse.setSelected(prefFindReverse.getBoolean());
		regExp.setSelected(prefRegExp.getBoolean());
		searchNodeNames.setSelected(prefSearchNodeNames.getBoolean());
		searchNodeVars.setSelected(prefSearchNodeVars.getBoolean());
		searchArcNames.setSelected(prefSearchArcNames.getBoolean());
		searchArcVars.setSelected(prefSearchArcVars.getBoolean());
		searchExportNames.setSelected(prefSearchExportNames.getBoolean());
		searchExportVars.setSelected(prefSearchExportVars.getBoolean());
		searchCellVars.setSelected(prefSearchCellVars.getBoolean());
		searchTempNames.setSelected(prefSearchTempNames.getBoolean());

        // adding correct callback
        ActionListener action = new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cleanLastSearch();
            }
        };
        searchNodeNames.addActionListener(action);
        searchNodeVars.addActionListener(action);
        searchArcNames.addActionListener(action);
        searchArcVars.addActionListener(action);
        searchExportNames.addActionListener(action);
        searchExportVars.addActionListener(action);
        searchCellVars.addActionListener(action);
        searchTempNames.addActionListener(action);

		getRootPane().setDefaultButton(find);
		finishInitialization();
	}

	private boolean badRegExpSyntax() {
        if (!regExp.isSelected()) return false;
        try {
            Pattern.compile(findString.getText());
            return false;
        } catch (Exception e) {
			System.out.println("Regular Expression error in Find string. Operation aborted.");
            return true;
        }
    }
    private Set<TextUtils.WhatToSearch> getWhatToSearch() {
    	Set<TextUtils.WhatToSearch> whatToSearch = new HashSet<TextUtils.WhatToSearch>();
		if (searchNodeNames.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.NODE_NAME);
		if (searchNodeVars.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.NODE_VAR);
		if (searchArcNames.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.ARC_NAME);
		if (searchArcVars.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.ARC_VAR);
		if (searchExportNames.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.EXPORT_NAME);
		if (searchExportVars.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.EXPORT_VAR);
		if (searchCellVars.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.CELL_VAR);
		if (searchTempNames.isSelected()) whatToSearch.add(TextUtils.WhatToSearch.TEMP_NAMES);
		return whatToSearch;
    }

	protected void escapePressed() { doneActionPerformed(null); }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel4 = new javax.swing.JLabel();
        Done = new javax.swing.JButton();
        findString = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        replaceString = new javax.swing.JTextField();
        caseSensitive = new javax.swing.JCheckBox();
        findReverse = new javax.swing.JCheckBox();
        replace = new javax.swing.JButton();
        replaceAndFind = new javax.swing.JButton();
        replaceAll = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        lineNumber = new javax.swing.JTextField();
        find = new javax.swing.JButton();
        goToLine = new javax.swing.JButton();
        regExp = new javax.swing.JCheckBox();
        whatToSearch = new javax.swing.JPanel();
        searchNodeNames = new javax.swing.JCheckBox();
        searchNodeVars = new javax.swing.JCheckBox();
        searchArcNames = new javax.swing.JCheckBox();
        searchArcVars = new javax.swing.JCheckBox();
        searchExportNames = new javax.swing.JCheckBox();
        searchExportVars = new javax.swing.JCheckBox();
        searchCellVars = new javax.swing.JCheckBox();
        searchTempNames = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Search and Replace");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jLabel4.setText("Find:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        Done.setMnemonic('d');
        Done.setText("Done");
        Done.setActionCommand("done");
        Done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(Done, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(findString, gridBagConstraints);

        jLabel1.setText("Line Number:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(replaceString, gridBagConstraints);

        caseSensitive.setMnemonic('c');
        caseSensitive.setText("Case Sensitive");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(caseSensitive, gridBagConstraints);

        findReverse.setMnemonic('v');
        findReverse.setText("Find Reverse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(findReverse, gridBagConstraints);

        replace.setMnemonic('r');
        replace.setText("Replace");
        replace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(replace, gridBagConstraints);

        replaceAndFind.setMnemonic('n');
        replaceAndFind.setText("Replace and Find");
        replaceAndFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceAndFindActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(replaceAndFind, gridBagConstraints);

        replaceAll.setMnemonic('a');
        replaceAll.setText("Replace All");
        replaceAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceAllActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(replaceAll, gridBagConstraints);

        jLabel2.setText("Replace:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        lineNumber.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(lineNumber, gridBagConstraints);

        find.setMnemonic('f');
        find.setText("Find");
        find.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(find, gridBagConstraints);

        goToLine.setMnemonic('g');
        goToLine.setText("Go To Line");
        goToLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goToLineActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(goToLine, gridBagConstraints);

        regExp.setMnemonic('e');
        regExp.setText("Regular Expressions");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        getContentPane().add(regExp, gridBagConstraints);

        whatToSearch.setLayout(new java.awt.GridBagLayout());

        whatToSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Objects to Search"));
        searchNodeNames.setText("Node Names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        whatToSearch.add(searchNodeNames, gridBagConstraints);

        searchNodeVars.setText("Node Variables");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        whatToSearch.add(searchNodeVars, gridBagConstraints);

        searchArcNames.setText("Arc Names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        whatToSearch.add(searchArcNames, gridBagConstraints);

        searchArcVars.setText("Arc Variables");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        whatToSearch.add(searchArcVars, gridBagConstraints);

        searchExportNames.setText("Export Names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        whatToSearch.add(searchExportNames, gridBagConstraints);

        searchExportVars.setText("Export Variables");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        whatToSearch.add(searchExportVars, gridBagConstraints);

        searchCellVars.setText("Cell Variables");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        whatToSearch.add(searchCellVars, gridBagConstraints);

        searchTempNames.setText("Automatically Generated Node and Arc Names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        whatToSearch.add(searchTempNames, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        getContentPane().add(whatToSearch, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doneActionPerformed
        closeDialog(null);
    }//GEN-LAST:event_doneActionPerformed

	private void goToLineActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_goToLineActionPerformed
	{//GEN-HEADEREND:event_goToLineActionPerformed
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		WindowContent content = wf.getContent();
		if (content instanceof TextWindow)
		{
			TextWindow tw = (TextWindow)content;
			int i = TextUtils.atoi(lineNumber.getText());
			tw.goToLineNumber(i);
			return;
		}
		System.out.println("Cannot access this window by line numbers");
	}//GEN-LAST:event_goToLineActionPerformed

	private void replaceAllActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_replaceAllActionPerformed
	{//GEN-HEADEREND:event_replaceAllActionPerformed
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		String search = findString.getText();
		String replace = replaceString.getText();
		WindowContent content = wf.getContent();
		content.initTextSearch(search, caseSensitive.isSelected(),
		                       regExp.isSelected(), getWhatToSearch());
		content.replaceAllText(replace);
	}//GEN-LAST:event_replaceAllActionPerformed

	private void replaceAndFindActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_replaceAndFindActionPerformed
	{//GEN-HEADEREND:event_replaceAndFindActionPerformed
                if (badRegExpSyntax()) return;
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		if (lastSearch == null) return;
		WindowContent content = wf.getContent();
		content.replaceText(replaceString.getText());
		if (!content.findNextText(findReverse.isSelected())) lastSearch = null;

	}//GEN-LAST:event_replaceAndFindActionPerformed

	private void replaceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_replaceActionPerformed
	{//GEN-HEADEREND:event_replaceActionPerformed
                if (badRegExpSyntax()) return;
                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		if (lastSearch == null) return;
		WindowContent content = wf.getContent();
		content.replaceText(replaceString.getText());
        replace.setEnabled(false);

	}//GEN-LAST:event_replaceActionPerformed

	private void findActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_findActionPerformed
	{//GEN-HEADEREND:event_findActionPerformed
                if (badRegExpSyntax()) return;
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		String search = findString.getText();
		WindowContent content = wf.getContent();
		if (lastSearch != null)
		{
			if (!lastSearch.equals(search)) lastSearch = null;
		}
		if (lastSearch == null)
		{
			content.initTextSearch(search, caseSensitive.isSelected(),
			                       regExp.isSelected(), getWhatToSearch());
		}
		lastSearch = search;
		if (!content.findNextText(findReverse.isSelected())) lastSearch = null;
        replace.setEnabled(true);
	}//GEN-LAST:event_findActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		prefFindTextMessage.setString(findString.getText());
		prefReplaceTextMessage.setString(replaceString.getText());
		prefCaseSensitive.setBoolean(caseSensitive.isSelected());
		prefFindReverse.setBoolean(findReverse.isSelected());
		prefRegExp.setBoolean(regExp.isSelected());
		prefSearchNodeNames.setBoolean(searchNodeNames.isSelected());
		prefSearchNodeVars.setBoolean(searchNodeVars.isSelected());
		prefSearchArcNames.setBoolean(searchArcNames.isSelected());
		prefSearchArcVars.setBoolean(searchArcVars.isSelected());
		prefSearchExportNames.setBoolean(searchExportNames.isSelected());
		prefSearchExportVars.setBoolean(searchExportVars.isSelected());
		prefSearchCellVars.setBoolean(searchCellVars.isSelected());
		prefSearchTempNames.setBoolean(searchTempNames.isSelected());

		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Done;
    private javax.swing.JCheckBox caseSensitive;
    private javax.swing.JButton find;
    private javax.swing.JCheckBox findReverse;
    private javax.swing.JTextField findString;
    private javax.swing.JButton goToLine;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JTextField lineNumber;
    private javax.swing.JCheckBox regExp;
    private javax.swing.JButton replace;
    private javax.swing.JButton replaceAll;
    private javax.swing.JButton replaceAndFind;
    private javax.swing.JTextField replaceString;
    private javax.swing.JCheckBox searchArcNames;
    private javax.swing.JCheckBox searchArcVars;
    private javax.swing.JCheckBox searchCellVars;
    private javax.swing.JCheckBox searchExportNames;
    private javax.swing.JCheckBox searchExportVars;
    private javax.swing.JCheckBox searchNodeNames;
    private javax.swing.JCheckBox searchNodeVars;
    private javax.swing.JCheckBox searchTempNames;
    private javax.swing.JPanel whatToSearch;
    // End of variables declaration//GEN-END:variables
}
