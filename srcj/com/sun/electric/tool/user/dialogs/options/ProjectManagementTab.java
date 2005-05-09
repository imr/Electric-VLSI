/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjectManagementTab.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.change.Undo;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.project.Project;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;

/**
 * Class to handle the "Project Management" tab of the Preferences dialog.
 */
public class ProjectManagementTab extends PreferencePanel
{
	private String initialRepository;
	private String initialUserName;
	private JList userList;
	private DefaultListModel userModel;
	private boolean authorized;

	/** Creates new form Edit Options */
	public ProjectManagementTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return projectManagement; }

	public String getName() { return "Project Management"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Project Management tab.
	 */
	public void init()
	{
		initialRepository = Project.getRepositoryLocation();
		repositoryTextArea.setText(initialRepository);
		initialUserName = Project.getCurrentUserName();
		currentUserLabel.setText("Current user: " + initialUserName);

		userModel = new DefaultListModel();
		userList = new JList(userModel);
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		userListPane.setViewportView(userList);
		userList.clearSelection();
		userModel.clear();
		for(Iterator it = Project.getUsers(); it.hasNext(); )
		{
			String user = (String)it.next();
			userModel.addElement(user);
		}
		userList.setSelectedIndex(0);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the General tab.
	 */
	public void term()
	{
		String currRepository = repositoryTextArea.getText();
		if (!currRepository.equals(initialRepository))
			Project.setRepositoryLocation(currRepository);
	}

	/**
	 * This class displays a dialog for password-related operations.
	 */
	private static class PasswordDialog extends EDialog
	{
		private static final int NEWUSER        = 1;
		private static final int CHANGEPASSWORD = 2;
		private static final int LOGINUSER      = 3;
		private static final int DELETEUSER     = 4;
		private static final int AUTHORIZE      = 5;

		private int operation;
		private String userName;
		private JTextField userNameField;
		private JPasswordField password, confirm, oldPassword;
		private boolean didCancel;

		/** Creates new form for password-related inquiries */
		private PasswordDialog(int operation, String userName)
		{
			super(null, true);
			this.operation = operation;
			this.userName = userName;
			initComponents();
			setVisible(true);
		}

		public boolean cancelled() { return didCancel; }

		public String getUserName() { return userNameField.getText(); }

		public String getPassword() { return new String(password.getPassword()); }

		protected void escapePressed() { exit(false); }

		private void exit(boolean goodButton)
		{
			didCancel = !goodButton;
			if (goodButton)
			{
				switch (operation)
				{
					case NEWUSER:
						// validate the dialog
						String name = userNameField.getText().trim();
						if (name.length() == 0)
						{
							JOptionPane.showMessageDialog(this, "You must type a user name", "Blank User Name", JOptionPane.ERROR_MESSAGE);
							userNameField.selectAll();
							return;
						}
						if (Project.isExistingUser(name))
						{
							JOptionPane.showMessageDialog(this, "User " + name + " already exists.  Choose another name",
								"User Name Exists", JOptionPane.ERROR_MESSAGE);
							userNameField.selectAll();
							return;
						}
						String pass = new String(password.getPassword());
						String conf = new String(confirm.getPassword());
						if (!pass.equals(conf))
						{
							JOptionPane.showMessageDialog(this, "Confirmed password does not match original password",
								"Confirmation Error", JOptionPane.ERROR_MESSAGE);
							confirm.selectAll();
							return;
						}
						break;

					case CHANGEPASSWORD:
						// validate the dialog
						String givenPassword = new String(oldPassword.getPassword()).trim();
						String realPassword = Project.getPassword(userName);
						if (!givenPassword.equals(realPassword))
						{
							JOptionPane.showMessageDialog(this, "Incorrect password given for user " + userName,
								"Invalid Password", JOptionPane.ERROR_MESSAGE);
							oldPassword.selectAll();
							return;
						}
						pass = new String(password.getPassword());
						conf = new String(confirm.getPassword());
						if (!pass.equals(conf))
						{
							JOptionPane.showMessageDialog(this, "Confirmed password does not match new password",
								"Confirmation Error", JOptionPane.ERROR_MESSAGE);
							confirm.selectAll();
							return;
						}
						break;

					case LOGINUSER:
						// validate the dialog
						givenPassword = new String(password.getPassword()).trim();
						realPassword = Project.getPassword(userName);
						if (!givenPassword.equals(realPassword))
						{
							JOptionPane.showMessageDialog(this, "Incorrect password given for user " + userName,
								"Invalid Password", JOptionPane.ERROR_MESSAGE);
							password.selectAll();
							return;
						}
						break;

					case AUTHORIZE:
						// validate the dialog
						givenPassword = new String(password.getPassword()).trim();
						realPassword = "e";
						if (!givenPassword.equals(realPassword))
						{
							JOptionPane.showMessageDialog(this, "Incorrect administrator password",
								"Invalid Password", JOptionPane.ERROR_MESSAGE);
							password.selectAll();
							return;
						}
						break;

				}
			}
			setVisible(false);
			dispose();
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			switch (operation)
			{
				case NEWUSER:
					setTitle("Create New User");
					JLabel lab1 = new JLabel("User name:");
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab1, gbc);

					userNameField = new JTextField("");
					userNameField.setColumns(10);
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(userNameField, gbc);

					JLabel lab2 = new JLabel("Password:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 1;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab2, gbc);

					password = new JPasswordField("");
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 1;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(password, gbc);

					JLabel lab3 = new JLabel("Confirm password:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 2;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab3, gbc);

					confirm = new JPasswordField("");
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 2;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(confirm, gbc);
					break;

				case CHANGEPASSWORD:
					setTitle("Change Password");
					lab1 = new JLabel("Old Password:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab1, gbc);

					oldPassword = new JPasswordField("");
					oldPassword.setColumns(10);
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(oldPassword, gbc);

					lab2 = new JLabel("New Password:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 1;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab2, gbc);

					password = new JPasswordField("");
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 1;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(password, gbc);

					lab3 = new JLabel("Confirm new password:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 2;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab3, gbc);

					confirm = new JPasswordField("");
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 2;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(confirm, gbc);
					break;

				case LOGINUSER:
					setTitle("Login");
					lab1 = new JLabel("User name:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab1, gbc);

					lab2 = new JLabel(userName);
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(lab2, gbc);

					lab3 = new JLabel("Password:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 1;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab3, gbc);

					password = new JPasswordField("");
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 1;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(password, gbc);
					break;

				case DELETEUSER:
					setTitle("Delete User");
					lab1 = new JLabel("Click OK to delete user: \"" + userName + "\"");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 0;
					gbc.gridwidth = 2;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab1, gbc);
					break;

				case AUTHORIZE:
					setTitle("Authorize");
					lab3 = new JLabel("Administrator password:");
					gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.WEST;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(lab3, gbc);

					password = new JPasswordField("");
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = 0;
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 1;
					gbc.insets = new Insets(4, 4, 4, 4);
					getContentPane().add(password, gbc);
					break;
			}

			// control buttons
			JButton cancel = new JButton("Cancel");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(true); }
			});
			getRootPane().setDefaultButton(ok);

			pack();
		}
	}

	private void reloadUsers()
	{
		userModel.clear();
		for(Iterator it = Project.getUsers(); it.hasNext(); )
		{
			String userName = (String)it.next();
			userModel.addElement(userName);
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        projectManagement = new javax.swing.JPanel();
        currentUserLabel = new javax.swing.JLabel();
        userListPane = new javax.swing.JScrollPane();
        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        deleteButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        authorizeButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        repositoryPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        browseButton = new javax.swing.JButton();
        repositoryTextArea = new javax.swing.JTextArea();
        loginButton = new javax.swing.JButton();
        passwordButton = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Project Management");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        projectManagement.setLayout(new java.awt.GridBagLayout());

        currentUserLabel.setText("Current User:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectManagement.add(currentUserLabel, gridBagConstraints);

        userListPane.setPreferredSize(new java.awt.Dimension(100, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectManagement.add(userListPane, gridBagConstraints);

        jLabel5.setText("Users:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectManagement.add(jLabel5, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("Administration"));
        deleteButton.setText("Delete User");
        deleteButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(deleteButton, gridBagConstraints);

        addButton.setText("Add User...");
        addButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                addButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(addButton, gridBagConstraints);

        authorizeButton.setText("Authorize...");
        authorizeButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                authorizeButtonActionPerformed(evt);
            }
        });

        jPanel1.add(authorizeButton, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        projectManagement.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectManagement.add(jSeparator1, gridBagConstraints);

        repositoryPanel.setLayout(new java.awt.GridBagLayout());

        repositoryPanel.setBorder(new javax.swing.border.TitledBorder("Repository"));
        jLabel1.setText("The repository contains a the latest version of your circuit,");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        repositoryPanel.add(jLabel1, gridBagConstraints);

        jLabel4.setText("Currently:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        repositoryPanel.add(jLabel4, gridBagConstraints);

        jLabel6.setText("and also contains a history of changes to each cell.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        repositoryPanel.add(jLabel6, gridBagConstraints);

        jLabel7.setText("It must be in a location that all users can access (on a network).");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        repositoryPanel.add(jLabel7, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        repositoryPanel.add(jSeparator2, gridBagConstraints);

        browseButton.setText("Browse");
        browseButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                browseButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        repositoryPanel.add(browseButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        repositoryPanel.add(repositoryTextArea, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        projectManagement.add(repositoryPanel, gridBagConstraints);

        loginButton.setText("Login");
        loginButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                loginButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectManagement.add(loginButton, gridBagConstraints);

        passwordButton.setText("Change Password");
        passwordButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                passwordButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        projectManagement.add(passwordButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        getContentPane().add(projectManagement, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteButtonActionPerformed
	{//GEN-HEADEREND:event_deleteButtonActionPerformed
		if (!authorized)
		{
			JOptionPane.showMessageDialog(this, "You must be authorized to delete users.  Click the 'Authorize' button.", "Not Authorized", JOptionPane.ERROR_MESSAGE);
			return;
		}
		int index = userList.getSelectedIndex();
		if (index < 0)
		{
			JOptionPane.showMessageDialog(this, "Select a user before clicking 'delete'", "Nothing Selected", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String user = (String)userModel.getElementAt(index);

		// get password
		PasswordDialog pwd = new PasswordDialog(PasswordDialog.DELETEUSER, user);
		if (pwd.cancelled()) return;

		// delete the user and redisplay
		Project.deleteUser(user);
		reloadUsers();
		// TODO add your handling code here:
	}//GEN-LAST:event_deleteButtonActionPerformed

	private void authorizeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_authorizeButtonActionPerformed
	{//GEN-HEADEREND:event_authorizeButtonActionPerformed
		PasswordDialog pwd = new PasswordDialog(PasswordDialog.AUTHORIZE, null);
		if (pwd.cancelled()) return;

		// allow authorized actions
		authorized = true;
	}//GEN-LAST:event_authorizeButtonActionPerformed

	private void browseButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_browseButtonActionPerformed
	{//GEN-HEADEREND:event_browseButtonActionPerformed
		String fileName = OpenFile.chooseDirectory(null);
		if (fileName == null) return;
		repositoryTextArea.setText(fileName);
	}//GEN-LAST:event_browseButtonActionPerformed

	private void addButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_addButtonActionPerformed
	{//GEN-HEADEREND:event_addButtonActionPerformed
		if (!authorized)
		{
			JOptionPane.showMessageDialog(this, "You must be authorized to add users.  Click the 'Authorize' button.", "Not Authorized", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// prompt for name and password
		PasswordDialog pwd = new PasswordDialog(PasswordDialog.NEWUSER, null);
		if (pwd.cancelled()) return;

		// create the user and redisplay
		String userName = pwd.getUserName();
		String password = pwd.getPassword();
		Project.addUser(userName, password);
		reloadUsers();
	}//GEN-LAST:event_addButtonActionPerformed

	private void passwordButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_passwordButtonActionPerformed
	{//GEN-HEADEREND:event_passwordButtonActionPerformed
		// find out the user to delete
		int index = userList.getSelectedIndex();
		if (index < 0)
		{
			JOptionPane.showMessageDialog(this, "Select a user before changing their password", "Nothing Selected", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String userName = (String)userModel.getElementAt(index);
	
		// prompt to change password (includes validation)
		PasswordDialog pwd = new PasswordDialog(PasswordDialog.CHANGEPASSWORD, userName);
		if (pwd.cancelled()) return;

		// make the change
		String password = pwd.getPassword();
		Project.changePassword(userName, password);
	}//GEN-LAST:event_passwordButtonActionPerformed

	private void loginButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loginButtonActionPerformed
	{//GEN-HEADEREND:event_loginButtonActionPerformed
		// find out the user to login
		int index = userList.getSelectedIndex();
		if (index < 0)
		{
			JOptionPane.showMessageDialog(this, "Select a user before clicking 'login'", "Nothing Selected", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String userName = (String)userModel.getElementAt(index);

		// get the password and validate it
		PasswordDialog pwd = new PasswordDialog(PasswordDialog.LOGINUSER, userName);
		if (pwd.cancelled()) return;

		// save this user as the one logged-in
		Project.setCurrentUserName(userName);
		currentUserLabel.setText("Current user: " + userName);
	}//GEN-LAST:event_loginButtonActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton authorizeButton;
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel currentUserLabel;
    private javax.swing.JButton deleteButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JButton loginButton;
    private javax.swing.JButton passwordButton;
    private javax.swing.JPanel projectManagement;
    private javax.swing.JPanel repositoryPanel;
    private javax.swing.JTextArea repositoryTextArea;
    private javax.swing.JScrollPane userListPane;
    // End of variables declaration//GEN-END:variables

}
