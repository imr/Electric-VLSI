/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: About.java
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

import com.sun.electric.database.text.Version;
import com.sun.electric.tool.user.Resources;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;


/**
 * Class to handle the "About" dialog.
 */
public class About extends EDialog
{
	private JList list;
	private DefaultListModel model;
	static private CastOfThousands [] showingCast = null;

	private static class CastOfThousands
	{
		private String name;
		private String work;

		private CastOfThousands(String name, String work) { this.name = name;   this.work = work; }

		static CastOfThousands [] javaTeam = new CastOfThousands[]
		{
			new CastOfThousands("Robert Bosnyak", 			"Pads library"),
			new CastOfThousands("Jonathan Gainsley", 		"User interface, Simulation, Logical effort"),
			new CastOfThousands("Gilda Garret\u00F3n", 		"DRC, ERC, 3D, technologies"),
			new CastOfThousands("David Harris", 			"ROM generator, Color printing"),
			new CastOfThousands("Jason Imada", 				"ROM generator"),
			new CastOfThousands("Russell Kao",				"NCC, generators, Hierarchy enumeration, Regressions"),
			new CastOfThousands("Frank Lee", 				"ROM generator"),
			new CastOfThousands("Ivan Minevskiy",			"NCC display"),
			new CastOfThousands("Dmitry Nadezhin", 			"Database, Networks, Libraries, Simulation, Optimizations"),
			new CastOfThousands("Ivan Sutherland", 			"Inspiration, NCC"),
			new CastOfThousands("Thomas Valine", 			"GDS output"),
		};
		static CastOfThousands [] theCast = new CastOfThousands[]
		{
			new CastOfThousands("Philip Attfield", 			"Box merging"),
			new CastOfThousands("Brett Bissinger", 		    "Node extraction"),
			new CastOfThousands("Ron Bolton", 				"Mathematical help"),
			new CastOfThousands("Robert Bosnyak", 			"Pads library"),
			new CastOfThousands("Mark Brinsmead", 			"Mathematical help"),
			new CastOfThousands("Stefano Concina", 			"Polygon clipping"),
			new CastOfThousands("Jonathan Gainsley", 		"User interface, Simulation, Logical effort"),
			new CastOfThousands("Peter Gallant", 			"ALS simulator"),
			new CastOfThousands("R. Brian Gardiner", 		"Electric lifeline"),
			new CastOfThousands("Gilda Garret\u00F3n", 		"DRC, ERC, 3D, technologies"),
			new CastOfThousands("T. J. Goodman", 			"Texsim output"),
			new CastOfThousands("Gerrit Groenewold", 		"SPICE parts"),
			new CastOfThousands("David Groulx", 		    "Node extraction"),
			new CastOfThousands("D. Guptill", 			    "X-window help"),
			new CastOfThousands("David Harris", 			"ROM generator, Color printing"),
			new CastOfThousands("Robert Hon", 				"CIF input parser"),
			new CastOfThousands("Jason Imada", 				"ROM generator"),
			new CastOfThousands("Sundaravarathan Iyengar", 	"nMOS PLA generator"),
			new CastOfThousands("Allan Jost", 				"VHDL compiler help, X-window help"),
			new CastOfThousands("Russell Kao",				"NCC, generators, Hierarchy enumeration, Regressions"),
			new CastOfThousands("Wallace Kroeker", 			"Digital filter technology, CMOS PLA generator"),
			new CastOfThousands("Andrew Kostiuk", 			"VHDL compiler, Silicon Compiler"),
			new CastOfThousands("Oliver Laumann", 			"ELK Lisp"),
			new CastOfThousands("Glen Lawson", 				"Maze routing, GDS input, EDIF I/O"),
			new CastOfThousands("Frank Lee", 				"ROM generator"),
			new CastOfThousands("Neil Levine", 				"PADS output"),
			new CastOfThousands("David Lewis", 				"Flat DRC checking"),
			new CastOfThousands("Erwin Liu", 				"Schematic and Round CMOS technology help"),
			new CastOfThousands("Dick Lyon", 				"MOSIS and Round CMOS technology help"),
			new CastOfThousands("John Mohammed", 			"Mathematical help"),
			new CastOfThousands("Mark Moraes", 				"Hierarchical DRC, X-window help"),
			new CastOfThousands("Dmitry Nadezhin", 			"Database, Networks, Libraries, Simulation, Optimizations"),
			new CastOfThousands("Sid Penstone", 			"SPICE, SILOS, GDS, Box merging, technologies"),
			new CastOfThousands("J. P. Polonovski", 		"Memory allocation help"),
			new CastOfThousands("Kevin Ryan", 			    "X-window help"),
			new CastOfThousands("Nora Ryan", 				"Compaction, technology conversion"),
			new CastOfThousands("Miguel Saro", 				"French translation"),
			new CastOfThousands("Brent Serbin", 			"ALS simulator"),
			new CastOfThousands("Ivan Sutherland", 			"Inspiration, NCC"),
			new CastOfThousands("Lyndon Swab", 				"HPGL output, SPICE output help, technologies"),
			new CastOfThousands("Brian W. Thomson", 		"Mimic stitcher, RSIM interface"),
			new CastOfThousands("Burnie West", 				"Bipolar technology, EDIF output help"),
			new CastOfThousands("Telle Whitney", 			"River router"),
			new CastOfThousands("Rob Winstanley", 			"CIF input, RNL output"),
			new CastOfThousands("Russell Wright", 			"SDF input, miscellaneous help"),
			new CastOfThousands("David J. Yurach",			"VHDL help")
		};
	}

	/** Creates the About dialog. */
	public About(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);

		version.setText(Version.getVersionInformation());
        // Just in case we modify information in Version..
		jLabel3.setText(Version.getAuthorInformation());
		jLabel9.setText(Version.getWarrantyInformation());
		jLabel8.setText(Version.getCopyrightInformation());

		// setup the region popup
		jComboBox1.addItem("N.America");
		jComboBox1.addItem("Australia,NZ");
		jComboBox1.addItem("Denmark");
		jComboBox1.addItem("Europe");
		jComboBox1.addItem("India");
		jComboBox1.addItem("Italy");
		jComboBox1.addItem("Israel");
		jComboBox1.addItem("Japan");
		jComboBox1.addItem("Russia");
		jComboBox1.addItem("Switzerland");
		jComboBox1.addItem("UK,Ireland");

		// make an empty list
		model = new DefaultListModel();
		list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Center.setViewportView(list);
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { listClick(evt); }
		});
		finishInitialization();
	}

	protected void escapePressed() { ok(null); }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        Center = new javax.swing.JScrollPane();
        Top = new javax.swing.JPanel();
        TopRight = new javax.swing.JPanel();
        theIcon = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        TopLeft = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        version = new javax.swing.JLabel();
        ok = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        castOfThousands = new javax.swing.JButton();
		javaTeam = new javax.swing.JButton();
        Bottom = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        warrantyDetails = new javax.swing.JButton();
        copyingDetails = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 10));

        setTitle("About Electric");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
				AboutWindowClosing(evt);
            }
        });

        Center.setMinimumSize(new java.awt.Dimension(100, 50));
        Center.setPreferredSize(new java.awt.Dimension(300, 200));
        getContentPane().add(Center, java.awt.BorderLayout.CENTER);

        Top.setLayout(new java.awt.BorderLayout(10, 0));

        TopRight.setLayout(new java.awt.GridBagLayout());

        theIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        theIcon.setMaximumSize(new java.awt.Dimension(64, 64));
        theIcon.setMinimumSize(new java.awt.Dimension(64, 64));
        theIcon.setPreferredSize(new java.awt.Dimension(64, 64));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        TopRight.add(theIcon, gridBagConstraints);

        jComboBox1.setMinimumSize(new java.awt.Dimension(100, 25));
        jComboBox1.setName("");
        jComboBox1.setPreferredSize(new java.awt.Dimension(100, 25));
        jComboBox1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                regionChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        TopRight.add(jComboBox1, gridBagConstraints);

        Top.add(TopRight, java.awt.BorderLayout.CENTER);

        TopLeft.setLayout(new java.awt.GridBagLayout());

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("The Electric VLSI Design System");
        jLabel4.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        TopLeft.add(jLabel4, gridBagConstraints);

        version.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        version.setText("Version 7.01a");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        TopLeft.add(version, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        TopLeft.add(ok, gridBagConstraints);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel3.setText("Written by Steven M. Rubin");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        TopLeft.add(jLabel3, gridBagConstraints);

		javaTeam.setText("The Java Team");
		javaTeam.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				showJavaTeam(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weighty = 0.1;
		gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
		TopLeft.add(javaTeam, gridBagConstraints);

		castOfThousands.setText("Cast of Thousands");
		castOfThousands.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				showCast(evt);
			}
		});

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        TopLeft.add(castOfThousands, gridBagConstraints);

        Top.add(TopLeft, java.awt.BorderLayout.WEST);

        getContentPane().add(Top, java.awt.BorderLayout.NORTH);

        Bottom.setLayout(new java.awt.GridBagLayout());

        jLabel9.setText("Electric comes with ABSOLUTELY NO WARRANTY");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Bottom.add(jLabel9, gridBagConstraints);

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("Copyright (c) 2004 Sun Microsystems and Static Free Software");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Bottom.add(jLabel8, gridBagConstraints);

        warrantyDetails.setText("Warranty Details");
        warrantyDetails.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                showWarranty(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Bottom.add(warrantyDetails, gridBagConstraints);

        copyingDetails.setText("Copying Details");
        copyingDetails.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                showCopying(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Bottom.add(copyingDetails, gridBagConstraints);

        jLabel11.setText("redistribute it under certain conditions");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        Bottom.add(jLabel11, gridBagConstraints);

        jLabel10.setText("This is free software, and you are welcome to");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        Bottom.add(jLabel10, gridBagConstraints);

        getContentPane().add(Bottom, java.awt.BorderLayout.SOUTH);

        pack();
    }//GEN-END:initComponents

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		setVisible(false);
		dispose();
	}//GEN-LAST:event_ok

	private void showCast(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showCast
	{//GEN-HEADEREND:event_showCast
		model.clear();
		for(int i=0; i<CastOfThousands.theCast.length; i++)
			model.addElement(CastOfThousands.theCast[i].name);
		showingCast = CastOfThousands.theCast;
	}//GEN-LAST:event_showCast

	private void showJavaTeam(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showJavaTeam
	{//GEN-HEADEREND:event_showJavaTeam
		model.clear();
		for(int i=0; i<CastOfThousands.javaTeam.length; i++)
			model.addElement(CastOfThousands.javaTeam[i].name);
		showingCast = CastOfThousands.javaTeam;
	}//GEN-LAST:event_showJavaTeam

	private void showCopying(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showCopying
	{//GEN-HEADEREND:event_showCopying
		// show the warranty
		String [] copyingString = new String[] {
			"TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION",
			"",
			"0. This License applies to any program or other work which contains a notice placed by",
			"the copyright holder saying it may be distributed under the terms of this General",
			"Public License. The 'Program', below, refers to any such program or work, and a",
			"'work based on the Program' means either the Program or any derivative work under",
			"copyright law: that is to say, a work containing the Program or a portion of it,",
			"either verbatim or with modifications and/or translated into another language.",
			"(Hereinafter, translation is included without limitation in the term 'modification'.)",
			"Each licensee is addressed as 'you'.",
			"",
			"Activities other than copying, distribution and modification are not covered by this",
			"License; they are outside its scope. The act of running the Program is not restricted,",
			"and the output from the Program is covered only if its contents constitute a work based",
			"on the Program (independent of having been made by running the Program). Whether that",
			"is true depends on what the Program does.",
			"",
			"1. You may copy and distribute verbatim copies of the Program's source code as you",
			"receive it, in any medium, provided that you conspicuously and appropriately publish",
			"on each copy an appropriate copyright notice and disclaimer of warranty; keep intact",
			"all the notices that refer to this License and to the absence of any warranty; and",
			"give any other recipients of the Program a copy of this License along with the Program.",
			"",
			"You may charge a fee for the physical act of transferring a copy, and you may at your",
			"option offer warranty protection in exchange for a fee.",
			"",
			"2. You may modify your copy or copies of the Program or any portion of it, thus forming",
			"a work based on the Program, and copy and distribute such modifications or work under",
			"the terms of Section 1 above, provided that you also meet all of these conditions:",
			"",
			"*	a) You must cause the modified files to carry prominent notices stating that you",
			"	changed the files and the date of any change.",
			"",
			"*	b) You must cause any work that you distribute or publish, that in whole or",
			"	in part contains or is derived from the Program or any part thereof, to be licensed",
			"	as a whole at no charge to all third parties under the terms of this License.",
			"",
			"*	c) If the modified program normally reads commands interactively when run, you",
			"	must cause it, when started running for such interactive use in the most ordinary",
			"	way, to print or display an announcement including an appropriate copyright notice",
			"	and a notice that there is no warranty (or else, saying that you provide a warranty)",
			"	and that users may redistribute the program under these conditions, and telling the",
			"	user how to view a copy of this License. (Exception: if the Program itself is",
			"	interactive but does not normally print such an announcement, your work based on the",
			"	Program is not required to print an announcement.)",
			"",
			"These requirements apply to the modified work as a whole. If identifiable sections",
			"of that work are not derived from the Program, and can be reasonably considered independent",
			"and separate works in themselves, then this License, and its terms, do not apply to those",
			"sections when you distribute them as separate works. But when you distribute the same",
			"sections as part of a whole which is a work based on the Program, the distribution of",
			"the whole must be on the terms of this License, whose permissions for other licensees",
			"extend to the entire whole, and thus to each and every part regardless of who wrote it.",
			"",
			"Thus, it is not the intent of this section to claim rights or contest your rights to",
			"work written entirely by you; rather, the intent is to exercise the right to control",
			"the distribution of derivative or collective works based on the Program.",
			"",
			"In addition, mere aggregation of another work not based on the Program with the Program",
			"(or with a work based on the Program) on a volume of a storage or distribution medium",
			"does not bring the other work under the scope of this License.",
			"",
			"3. You may copy and distribute the Program (or a work based on it, under Section 2)",
			"in object code or executable form under the terms of Sections 1 and 2 above provided",
			"that you also do one of the following:",
			"",
			"*	a) Accompany it with the complete corresponding machine-readable source code,",
			"which must be distributed under the terms of Sections 1 and 2 above on a medium",
			"customarily used for software interchange; or,",
			"",
			"*	b) Accompany it with a written offer, valid for at least three years, to give",
			"any third party, for a charge no more than your cost of physically performing source",
			"distribution, a complete machine-readable copy of the corresponding source code,",
			"to be distributed under the terms of Sections 1 and 2 above on a medium customarily",
			"used for software interchange; or,",
			"",
			"*	c) Accompany it with the information you received as to the offer to distribute",
			"corresponding source code. (This alternative is allowed only for noncommercial",
			"distribution and only if you received the program in object code or executable",
			"form with such an offer, in accord with Subsection b above.)",
			"",
			"The source code for a work means the preferred form of the work for making",
			"modifications to it. For an executable work, complete source code means all",
			"the source code for all modules it contains, plus any associated interface",
			"definition files, plus the scripts used to control compilation and installation",
			"of the executable. However, as a special exception, the source code distributed",
			"need not include anything that is normally distributed (in either source or binary",
			"form) with the major components (compiler, kernel, and so on) of the operating",
			"system on which the executable runs, unless that component itself accompanies the executable.",
			"",
			"If distribution of executable or object code is made by offering access to copy",
			"from a designated place, then offering equivalent access to copy the source code",
			"from the same place counts as distribution of the source code, even though third",
			"parties are not compelled to copy the source along with the object code.",
			"",
			"4. You may not copy, modify, sublicense, or distribute the Program except as",
			"expressly provided under this License. Any attempt otherwise to copy, modify,",
			"sublicense or distribute the Program is void, and will automatically terminate your",
			"rights under this License. However, parties who have received copies, or rights,",
			"from you under this License will not have their licenses terminated so long as",
			"such parties remain in full compliance.",
			"",
			"5. You are not required to accept this License, since you have not signed it.",
			"However, nothing else grants you permission to modify or distribute the Program or",
			"its derivative works. These actions are prohibited by law if you do not accept this",
			"License. Therefore, by modifying or distributing the Program (or any work based on",
			"the Program), you indicate your acceptance of this License to do so, and all its",
			"terms and conditions for copying, distributing or modifying the Program or works based on it.",
			"",
			"6. Each time you redistribute the Program (or any work based on the Program),",
			"the recipient automatically receives a license from the original licensor to copy,",
			"distribute or modify the Program subject to these terms and conditions. You may not",
			"impose any further restrictions on the recipients' exercise of the rights granted",
			"herein. You are not responsible for enforcing compliance by third parties to this License.",
			"",
			"7. If, as a consequence of a court judgment or allegation of patent infringement",
			"or for any other reason (not limited to patent issues), conditions are imposed",
			"on you (whether by court order, agreement or otherwise) that contradict the conditions",
			"of this License, they do not excuse you from the conditions of this License. If you",
			"cannot distribute so as to satisfy simultaneously your obligations under this",
			"License and any other pertinent obligations, then as a consequence you may not",
			"distribute the Program at all. For example, if a patent license would not permit",
			"royalty-free redistribution of the Program by all those who receive copies directly",
			"or indirectly through you, then the only way you could satisfy both it and this",
			"License would be to refrain entirely from distribution of the Program.",
			"",
			"If any portion of this section is held invalid or unenforceable under any",
			"particular circumstance, the balance of the section is intended to apply and",
			"the section as a whole is intended to apply in other circumstances.",
			"",
			"It is not the purpose of this section to induce you to infringe any patents",
			"or other property right claims or to contest validity of any such claims; this",
			"section has the sole purpose of protecting the integrity of the free software",
			"distribution system, which is implemented by public license practices. Many",
			"people have made generous contributions to the wide range of software distributed",
			"through that system in reliance on consistent application of that system; it is",
			"up to the author/donor to decide if he or she is willing to distribute software",
			"through any other system and a licensee cannot impose that choice.",
			"",
			"This section is intended to make thoroughly clear what is believed to be a",
			"consequence of the rest of this License.",
			"",
			"8. If the distribution and/or use of the Program is restricted in certain",
			"countries either by patents or by copyrighted interfaces, the original copyright",
			"holder who places the Program under this License may add an explicit geographical",
			"distribution limitation excluding those countries, so that distribution is permitted",
			"only in or among countries not thus excluded. In such case, this License incorporates",
			"the limitation as if written in the body of this License.",
			"",
			"9. The Free Software Foundation may publish revised and/or new versions of the",
			"General Public License from time to time. Such new versions will be similar in",
			"spirit to the present version, but may differ in detail to address new problems",
			"or concerns.",
			"",
			"Each version is given a distinguishing version number. If the Program specifies",
			"a version number of this License which applies to it and 'any later version',",
			"you have the option of following the terms and conditions either of that version",
			"or of any later version published by the Free Software Foundation. If the Program",
			"does not specify a version number of this License, you may choose any version ever",
			"published by the Free Software Foundation.",
			"",
			"10. If you wish to incorporate parts of the Program into other free programs",
			"whose distribution conditions are different, write to the author to ask for",
			"permission. For software which is copyrighted by the Free Software Foundation,",
			"write to the Free Software Foundation; we sometimes make exceptions for this.",
			"Our decision will be guided by the two goals of preserving the free status of",
			"all derivatives of our free software and of promoting the sharing and reuse of",
			"software generally."};
		model.clear();
		for(int i=0; i<copyingString.length; i++)
			model.addElement(copyingString[i]);
		showingCast = null;
	}//GEN-LAST:event_showCopying

	private void showWarranty(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showWarranty
	{//GEN-HEADEREND:event_showWarranty
		// show the warranty
		String [] warrantyString = new String[] {
			"NO WARRANTY",
			"",
			"11. Because the program is licensed free of charge, there is no warranty for the",
			"program, to the extent permitted by applicable law. Except when otherwise stated",
			"in writing the copyright holders and/or other parties provide the program 'as is'",
			"without warranty of any kind, either expressed or implied, including, but not",
			"limited to, the implied warranties of merchantability and fitness for a particular",
			"purpose. The entire risk as to the quality and performance of the program is with you.",
			"Should the program prove defective, you assume the cost of all necessary servicing,",
			"repair or correction.",
			"",
			"12. In no event unless required by applicable law or agreed to in writing will any",
			"copyright holder, or any other party who may modify and/or redistribute the program",
			"as permitted above, be liable to you for damages, including any general, special,",
			"incidental or consequential damages arising out of the use or inability to use the",
			"program (including but not limited to loss of data or data being rendered inaccurate",
			"or losses sustained by you or third parties or a failure of the program to operate",
			"with any other programs), even if such holder or other party has been advised of",
			"the possibility of such damages."};
		model.clear();
		for(int i=0; i<warrantyString.length; i++)
			model.addElement(warrantyString[i]);
		showingCast = null;
	}//GEN-LAST:event_showWarranty

	private void regionChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_regionChanged
	{//GEN-HEADEREND:event_regionChanged
		// the popup of socket regions changed
		JComboBox cb = (JComboBox)evt.getSource();
		int index = cb.getSelectedIndex();
		String socketName = null;
		switch (index)
		{
			case 0:  socketName = "SocketNAmerica.gif";     break;
			case 1:  socketName = "SocketAustralia.gif";    break;
			case 2:  socketName = "SocketDenmark.gif";      break;
			case 3:  socketName = "SocketEurope.gif";       break;
			case 4:  socketName = "SocketIndia.gif";        break;
			case 5:  socketName = "SocketItaly.gif";        break;
			case 6:  socketName = "SocketIsrael.gif";       break;
			case 7:  socketName = "SocketJapan.gif";        break;
			case 8:  socketName = "SocketRussia.gif";       break;
			case 9:  socketName = "SocketSwitzerland.gif";  break;
			case 10: socketName = "SocketUK.gif";           break;
		}
		if (socketName != null)
			theIcon.setIcon(Resources.getResource(getClass(), socketName));
	}//GEN-LAST:event_regionChanged
	
	/** Closes the dialog */
	private void AboutWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	private void listClick(java.awt.event.MouseEvent evt)
	{
		if (showingCast == null) return;
		int index = list.getSelectedIndex();
		model.setElementAt(showingCast[index].name + ": " + showingCast[index].work, index);
	}
	
    // Variables declaration - do not modify
    private JPanel Bottom;
    private JScrollPane Center;
    private JPanel Top;
    private JPanel TopLeft;
    private JPanel TopRight;
	private JButton castOfThousands;
	private JButton javaTeam;
    private JButton copyingDetails;
    private JComboBox jComboBox1;
    private JLabel jLabel10;
    private JLabel jLabel11;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel8;
    private JLabel jLabel9;
    private JButton ok;
    private JLabel theIcon;
    private JLabel version;
    private JButton warrantyDetails;
}
