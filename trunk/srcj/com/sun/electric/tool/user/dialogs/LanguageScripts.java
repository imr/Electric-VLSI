/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LanguageScripts.java
 * Manage the list of Bean Shell scripts attached to the menu.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.ToolMenu;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Language Scripts" dialog.
 */
public class LanguageScripts extends EDialog
{
	private JList scriptsList;
	private DefaultListModel scriptsModel;
	private boolean changingMnemonic;
	private static Pref prefScriptList = Pref.makeStringPref("BoundScripts", User.getUserTool().prefs, "");

	public static class ScriptBinding
	{
		public char mnemonic;
		public String fileName;
	}

	/** Creates new form Language Scripts */
	public LanguageScripts()
	{
		super(TopLevel.getCurrentJFrame(), true);
		initComponents();

		scriptsModel = new DefaultListModel();
		scriptsList = new JList(scriptsModel);
		scriptsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scriptsPane.setViewportView(scriptsList);
		List<ScriptBinding> scripts = getScripts();
		for(ScriptBinding s : scripts)
		{
			String scriptLine = makeLine(s);
			scriptsModel.addElement(scriptLine);
		}
		mnemonic.getDocument().addDocumentListener(new MnemonicDocumentListener());
		scriptsList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { clickedScript(); }
		});
		pack();
		finishInitialization();
		setVisible(true);
	}

	protected void escapePressed() { closeDialog(null); }

	public static List<ScriptBinding> getScripts()
	{
		List<ScriptBinding> scripts = new ArrayList<ScriptBinding>();
		String allScripts = prefScriptList.getString();
		String[] eachScript = allScripts.split("\t");
		for(int i=0; i<eachScript.length; i++)
		{
			String script = eachScript[i].trim();
			if (script.length() == 0) continue;
			ScriptBinding sb = new ScriptBinding();
			if (script.charAt(1) == '/')
			{
				sb.fileName = script.substring(2);
				sb.mnemonic = script.charAt(0);
			} else
			{
				sb.fileName = script;
				sb.mnemonic = 0;
			}
			scripts.add(sb);
		}
		return scripts;
	}

	public void saveScripts()
	{
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<scriptsModel.getSize(); i++)
		{
			if (sb.length() > 0) sb.append('\t');
			String scriptLine = (String)scriptsModel.elementAt(i);
			if (scriptLine.charAt(1) == ':')
				scriptLine = scriptLine.substring(0, 1) + "/" + scriptLine.substring(3);
			scriptLine = scriptLine.trim();
			sb.append(scriptLine);
		}
		prefScriptList.setString(sb.toString());
	}

	/**
	 * Class to handle special changes to changes to a the Mnemonic letter.
	 */
	private class MnemonicDocumentListener implements DocumentListener
	{
		public void changedUpdate(DocumentEvent e) { changedMnemonic(); }
		public void insertUpdate(DocumentEvent e) { changedMnemonic(); }
		public void removeUpdate(DocumentEvent e) { changedMnemonic(); }
	}

	private void clickedScript()
	{
		ScriptBinding sb = getSelected();
		if (sb == null) return;
		changingMnemonic = true;
		if (sb.mnemonic == 0) mnemonic.setText(""); else
			mnemonic.setText(sb.mnemonic+"");
		changingMnemonic = false;
	}

	private String makeLine(ScriptBinding sb)
	{
		String scriptLine = sb.fileName;
		if (sb.mnemonic != 0) scriptLine = sb.mnemonic + ": " + scriptLine; else
			scriptLine = "   " + scriptLine;
		return scriptLine;
	}

	private ScriptBinding getSelected()
	{
		int index = scriptsList.getSelectedIndex();
		if (index < 0 || index >= scriptsModel.size()) return null;
		ScriptBinding sb = new ScriptBinding();
		String scriptLine = (String)scriptsModel.elementAt(index);
		sb.fileName = scriptLine.substring(3);
		if (scriptLine.charAt(1) == ':')
		{
			sb.mnemonic = scriptLine.charAt(0);
		}
		return sb;
	}

	private void changedMnemonic()
	{
		if (changingMnemonic) return;
		ScriptBinding sb = getSelected();
		if (sb == null) return;
		String mn = mnemonic.getText().trim();
		if (mn.length() == 0) sb.mnemonic = 0; else
			sb.mnemonic = mn.charAt(0);
		scriptsModel.set(scriptsList.getSelectedIndex(), makeLine(sb));
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        ok = new javax.swing.JButton();
        scriptsPane = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        removeScript = new javax.swing.JButton();
        addScript = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        mnemonic = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Language Scripts");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        scriptsPane.setPreferredSize(new java.awt.Dimension(300, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(scriptsPane, gridBagConstraints);

        jLabel2.setText("Scripts:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        removeScript.setText("Remove Script");
        removeScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeScriptActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(removeScript, gridBagConstraints);

        addScript.setText("Add Script");
        addScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addScriptActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(addScript, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        mnemonic.setColumns(4);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0.2;
        getContentPane().add(mnemonic, gridBagConstraints);

        jLabel1.setText("Mnemonic:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0.2;
        getContentPane().add(jLabel1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
		closeDialog(null);
    }//GEN-LAST:event_cancelActionPerformed

    private void addScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addScriptActionPerformed
        String fileName = OpenFile.chooseInputFile(FileType.JAVA, null);
        if (fileName != null)
        {
        	scriptsModel.addElement("   " + fileName);
        	scriptsList.setSelectedIndex(scriptsModel.size()-1);
        }
    }//GEN-LAST:event_addScriptActionPerformed

    private void removeScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeScriptActionPerformed
    	int index = scriptsList.getSelectedIndex();
    	if (index < 0 || index >= scriptsModel.size()) return;
    	scriptsModel.remove(index);
    }//GEN-LAST:event_removeScriptActionPerformed

    private void okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okActionPerformed
    	saveScripts();
    	ToolMenu.setDynamicLanguageMenu();
		closeDialog(null);
    }//GEN-LAST:event_okActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addScript;
    private javax.swing.JButton cancel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField mnemonic;
    private javax.swing.JButton ok;
    private javax.swing.JButton removeScript;
    private javax.swing.JScrollPane scriptsPane;
    // End of variables declaration//GEN-END:variables
}
