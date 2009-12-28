/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AddedTechnologiesTab.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * Class to handle the "Added Technologies" tab of the Preferences dialog.
 */
public class AddedTechnologiesTab extends PreferencePanel
{
    private JPanel addedTechnologies;
	private JList addedTechnologiesList;
	private DefaultListModel addedTechnologiesModel;
    private JScrollPane addedTechnologiesPane;
	private Setting softTechnologiesSetting = User.getSoftTechnologiesSetting();
    private JButton removeTech;

    /** Creates new form AddedTechnologiesTab */
	public AddedTechnologiesTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the JPanel to use for the project preferences part of this tab. */
	public JPanel getProjectPreferencesPanel() { return addedTechnologies; }

	/** return the name of this Project Preferences tab. */
	public String getName() { return "Added Technologies"; }

    /**
	 * Method called at the start of the Added Technologies tab.
	 */
    @Override
	public void init()
	{
		addedTechnologiesModel = new DefaultListModel();
		addedTechnologiesList = new JList(addedTechnologiesModel);
		addedTechnologiesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		addedTechnologiesPane.setViewportView(addedTechnologiesList);
		for(String techPath: getString(softTechnologiesSetting).split(";"))
			if (techPath.length() > 0) addedTechnologiesModel.addElement(techPath);

        // to detect proper selection
        addedTechnologiesList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                setRemoveButton();
            }
        });
        setRemoveButton();
    }

    private void setRemoveButton()
    {
        removeTech.setEnabled(addedTechnologiesList.getSelectedIndex() != -1);
    }

    /**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Added Technologies tab.
	 */
    @Override
	public void term()
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < addedTechnologiesModel.size(); i++)
		{
			if (i != 0) sb.append(";");
			sb.append(addedTechnologiesModel.get(i));
		}
        setString(softTechnologiesSetting, sb.toString());
	}

    public void reset()
    {
    }

	private void addTechnology()
	{
		String fileName = OpenFile.chooseInputFile(FileType.XML, null);
		if (fileName == null) return;
		addedTechnologiesModel.addElement(fileName);
        setRemoveButton();
    }

	private void removeTechnology()
	{
		int line = addedTechnologiesList.getSelectedIndex();
		if (line < 0) return;
		addedTechnologiesModel.remove(line);
        setRemoveButton();
    }

	/** This method is called from within the constructor to
	 * initialize the form.
	 */
    private void initComponents()
    {
    	GridBagConstraints gridBagConstraints;

    	addedTechnologies = new JPanel();

        getContentPane().setLayout(new GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent evt) { closeDialog(evt); }
        });

        addedTechnologies.setLayout(new GridBagLayout());

        JLabel jLabel1 = new JLabel("Technologies that will be added to Electric when it next runs");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 1, 4);
        addedTechnologies.add(jLabel1, gridBagConstraints);
        JLabel jLabel2 = new JLabel("(these are XML files created by the Technology Editor)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(1, 4, 4, 4);
        addedTechnologies.add(jLabel2, gridBagConstraints);

        addedTechnologiesPane = new JScrollPane();
        addedTechnologiesPane.setPreferredSize(new Dimension(300, 200));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        addedTechnologies.add(addedTechnologiesPane, gridBagConstraints);

        JButton addTech = new JButton("Add");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        addedTechnologies.add(addTech, gridBagConstraints);
        addTech.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { addTechnology(); }
		});

		removeTech = new JButton("Remove");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        addedTechnologies.add(removeTech, gridBagConstraints);
        removeTech.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { removeTechnology(); }
		});

        getContentPane().add(addedTechnologies, new GridBagConstraints());

        pack();
    }

	/** Closes the dialog */
	private void closeDialog(WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}
}
