/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ComponentMenuTab.java
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

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Component Menu" dialog.
 */
public class ComponentMenuTab extends PreferencePanel
{
	private JList listNodes, listArcs, listSpecials, listPopup;
	private DefaultListModel modelNodes, modelArcs, modelSpecials, modelPopup;
	private int menuWid, menuHei, menuSelectedX, menuSelectedY;
	private int lastListSelected = -1;
	private Object [][] menuArray;
	private MenuView menuView;
	private Technology tech;
	private boolean changingNodeFields = false;
	private boolean changed;

	/** Creates new form ComponentMenu */
	public ComponentMenuTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		menuView = new MenuView();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 1;   gbc.gridheight = 6;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;   gbc.weighty = 0.9;
		gbc.insets = new Insets(1, 4, 4, 4);
		Top.add(menuView, gbc);

		// load the node function combobox
		List<PrimitiveNode.Function> funs = PrimitiveNode.Function.getFunctions();
		for(PrimitiveNode.Function fun : funs)
			nodeFunction.addItem(fun.getName());

		// make the nodes, arcs, specials, and popup lists
		modelNodes = new DefaultListModel();
		listNodes = new JList(modelNodes);
		nodeListPane.setViewportView(listNodes);
		listNodes.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { selectList(0); }
		});

		modelArcs = new DefaultListModel();
		listArcs = new JList(modelArcs);
		arcListPane.setViewportView(listArcs);
		listArcs.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { selectList(1); }
		});

		modelSpecials = new DefaultListModel();
		listSpecials = new JList(modelSpecials);
		specialListPane.setViewportView(listSpecials);
		listSpecials.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { selectList(2); }
		});

		modelPopup = new DefaultListModel();
		listPopup = new JList(modelPopup);
		popupListPane.setViewportView(listPopup);
		listPopup.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { showSelectedPopup(); }
		});

		// setup listeners for the node detail fields
		nodeAngle.getDocument().addDocumentListener(new NodeFieldDocumentListener());
		nodeName.getDocument().addDocumentListener(new NodeFieldDocumentListener());
		nodeTextSize.getDocument().addDocumentListener(new NodeFieldDocumentListener());
		nodeFunction.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { nodeInfoChanged(); }
		});
		showNodeName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) { nodeInfoChanged(); }
		});
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return Top; }

	/** return the name of this preferences tab. */
	public String getName() { return "Component Menu"; }

	/**
	 * Method called at the start of the dialog.
	 */
	public void init()
	{
		tech = Technology.getCurrent();
		showTechnology();
		changed = false;

//		finishInitialization();
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the tab.
	 */
	public void term()
	{
		if (!changed) return;
		Xml.MenuPalette xmp = new Xml.MenuPalette();
		xmp.numColumns = menuWid;
		xmp.menuBoxes = new ArrayList<List<Object>>();
		for(int y=0; y<menuHei; y++)
		{
			for(int x=0; x<menuWid; x++)
			{
				Object item = null;
				if (menuArray[y] != null)
					item = menuArray[y][x];
				if (item instanceof List)
				{
					List<Object> subList = new ArrayList<Object>();
					for(Object it : (List)item)
						subList.add(convertToXML(it));
					xmp.menuBoxes.add(subList);
				} else
				{
					List<Object> subList = new ArrayList<Object>();
					Object single = convertToXML(item);
					if (single != null) subList.add(single);
					xmp.menuBoxes.add(subList);
				}
			}
		}
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		MyWriter writer = new MyWriter(out);
		writer.writeMenuPaletteXml(xmp);
		out.close();
		StringBuffer sb = sw.getBuffer();
		tech.setNodesGrouped(menuArray, sb.toString());

		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			wf.getPaletteTab().loadForTechnology(tech, wf);
		}
	}

	private static class MyWriter extends Xml.Writer
	{
		public MyWriter(PrintWriter out)
		{
			super(out);
		}

		/**
		 * Print text without replacement of special chars.
		 */
		protected void p(String s)
		{
			for (int i = 0; i < s.length(); i++)
				out.print(s.charAt(i));
		}

		protected void checkIndent() { indentEmitted = true; }

		/**
		 * Do not print new line.
		 */
		protected void l() { indentEmitted = false; }
	}

	private Object convertToXML(Object obj)
	{
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			Xml.MenuNodeInst xni = new Xml.MenuNodeInst();
			xni.protoName = ni.getProto().getName();
			xni.function = ni.getFunction();
			Variable var = ni.getVar(Technology.TECH_TMPVAR);
			if (var != null)
			{
				xni.text = (String)var.getObject();
				xni.fontSize = var.getTextDescriptor().getSize().getSize();
			}
			xni.rotation = ni.getAngle();
			return xni;
		} else if (obj instanceof NodeProto)
		{
			NodeProto np = (NodeProto)obj;
			Xml.PrimitiveNode xnp = new Xml.PrimitiveNode();
			xnp.name = np.getName();
			return xnp;
		} else if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			Xml.ArcProto xap = new Xml.ArcProto();
			xap.name = ap.getName();
			return xap;
		}
		return obj;
	}

	/**
	 * Class to handle special changes to changes to node fields.
	 */
	private class NodeFieldDocumentListener implements DocumentListener
	{
		public void changedUpdate(DocumentEvent e) { nodeInfoChanged(); }
		public void insertUpdate(DocumentEvent e) { nodeInfoChanged(); }
		public void removeUpdate(DocumentEvent e) { nodeInfoChanged(); }
	}

	/**
	 * Method called with one of the lists on the right is clicked.
	 * Remembers the last list clicked (node, arc, or special) so that
	 * the "add" button will take from the right list.
	 * @param list the list selected (0=node, 1=arc, 2=special).
	 */
	private void selectList(int list)
	{
		lastListSelected = list;
		switch (list)
		{
			case 0:
				listArcs.clearSelection();
				listSpecials.clearSelection();
				break;
			case 1:
				listNodes.clearSelection();
				listSpecials.clearSelection();
				break;
			case 2:
				listNodes.clearSelection();
				listArcs.clearSelection();
				break;
		}
	}

	/**
	 * Method to load the dialog with information about the technology.
	 */
	private void showTechnology()
	{
		modelNodes.clear();
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = it.next();
			modelNodes.addElement(pnp.getName());
		}

		modelArcs.clear();
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			modelArcs.addElement(ap.getName());
		}

		modelSpecials.clear();
		modelSpecials.addElement("Cell");
		modelSpecials.addElement("Export");
		modelSpecials.addElement("Misc");
		modelSpecials.addElement("Pure");
		modelSpecials.addElement("Spice");

		// display the menu
		menuArray = tech.getNodesGrouped(null);
		menuWid = menuArray[0].length;
		menuHei = menuArray.length;
		showMenuSize();
		showSelected();
		menuView.repaint();
	}

	private void showMenuSize()
	{
		menuSize.setText(tech.getTechName() + " Component menu (" + menuWid + " by " + menuHei + ")");
	}

	/**
	 * Method to show details about the selected menu entry.
	 */
	private void showSelected()
	{
		Object item = (menuArray[menuSelectedY] == null) ? null : menuArray[menuSelectedY][menuSelectedX];
		showSelectedObject(item, false);
	}

	/**
	 * Method to show details about an object in a menu entry.
	 * @param item the object to show in detail.
	 * @param fromPopup true if this is from a popup list.
	 */
	private void showSelectedObject(Object item, boolean fromPopup)
	{
		showThisNode(false, null, null);
		if (!fromPopup)
		{
			popupListPane.setViewportView(null);
			modelPopup.clear();
		}
		if (item instanceof PrimitiveNode)
		{
			PrimitiveNode np = (PrimitiveNode)item;
			if (!fromPopup) selectedMenuName.setText("Node entry: " + np.getName());
			showThisNode(true, null, np);
		} else if (item instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)item;
			String name = "Node entry: " + getNodeName(ni);
			if (!fromPopup) selectedMenuName.setText(name);
			showThisNode(true, ni, ni.getProto());
		} else if (item instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)item;
			if (!fromPopup) selectedMenuName.setText("Arc entry: " + ap.getName());
		} else if (item instanceof List && !fromPopup)
		{
			selectedMenuName.setText("Popup menu entry:");
			popupListPane.setViewportView(listPopup);
			List nodes = (List)item;
			for(Object obj : nodes)
			{
				if (obj instanceof NodeInst) modelPopup.addElement(getNodeName((NodeInst)obj)); else
					modelPopup.addElement(obj);
			}
		} else if (item instanceof String)
		{
			if (!fromPopup) selectedMenuName.setText("Special entry: " + (String)item);
		} else
		{
			if (!fromPopup) selectedMenuName.setText("Empty entry");
		}
	}

	/**
	 * Method called when the user clicks on an entry in a Popup list.
	 */
	private void showSelectedPopup()
	{
		Object item = (menuArray[menuSelectedY] == null) ? null : menuArray[menuSelectedY][menuSelectedX];
		if (item == null) return;
		if (item instanceof List)
		{
			List nodes = (List)item;
			int index = listPopup.getSelectedIndex();
			if (index < 0 || index >= nodes.size()) return;
			Object obj = nodes.get(index);
			showSelectedObject(obj, true);
		}
	}

	/**
	 * Method to determine the name of a node (may use its displayed name).
	 * @param ni the node to describe.
	 * @return the name to use for that node.
	 */
	private String getNodeName(NodeInst ni)
	{
		Variable var = ni.getVar(Technology.TECH_TMPVAR);
		if (var != null && !var.isDisplay())
		{
			String dispName = (String)var.getObject();
			if (dispName.trim().length() > 0) return dispName;
		}
		return ni.getProto().getName();
	}

	private void nodeInfoChanged()
	{
		if (changingNodeFields) return;
		Object item = (menuArray[menuSelectedY] == null) ? null : menuArray[menuSelectedY][menuSelectedX];
		if (item == null) return;
		int index = -1;
		List nodes = null;
		if (item instanceof List)
		{
			nodes = (List)item;
			index = listPopup.getSelectedIndex();
			if (index < 0 || index >= nodes.size()) return;
			item = nodes.get(index);
		}
		NodeProto np;
		if (item instanceof NodeInst) np = ((NodeInst)item).getProto(); else
			if (item instanceof NodeProto) np = (NodeProto)item; else return;
		int angle = TextUtils.atoi(nodeAngle.getText()) * 10;
		boolean showName = showNodeName.isSelected();
		String dispName = nodeName.getText();
		double textSize = TextUtils.atof(nodeTextSize.getText());
		PrimitiveNode.Function fun = PrimitiveNode.Function.findName((String)nodeFunction.getSelectedItem());
		Object newItem = Technology.makeNodeInst(np, fun, angle, showName, dispName, textSize);
		if (index < 0)
		{
			menuArray[menuSelectedY][menuSelectedX] = newItem;
		} else
		{
			nodes.set(index, newItem);
		}
		menuView.repaint();
		changed = true;
	}

	/**
	 * Method to show details about the selected node.
	 * @param valid true if this is a valid node (false to dim all detail fields).
	 * @param ni the NodeInst (may be null but still have a valid prototype).
	 * @param np the NodeProto.
	 */
	private void showThisNode(boolean valid, NodeInst ni, NodeProto np)
	{
		changingNodeFields = true;
		showNodeName.setSelected(false);
		nodeName.setText("");
		nodeTextSize.setText("");
		nodeAngle.setText("");
		nodeFunction.setSelectedIndex(0);
		if (valid)
		{
			nodeAngle.setEnabled(true);
			nodeAngleLabel.setEnabled(true);
			nodeFunction.setEnabled(true);
			nodeFunctionLabel.setEnabled(true);
			nodeName.setEnabled(true);
			nodeNameLabel.setEnabled(true);
			nodeTextSize.setEnabled(true);
			nodeTextSizeLabel.setEnabled(true);
			showNodeName.setEnabled(true);
			if (ni == null)
			{
				nodeAngle.setText("0");
				nodeTextSize.setText("4");
				if (np != null) nodeFunction.setSelectedItem(np.getFunction().getName());
			} else
			{
				nodeAngle.setText((ni.getOrient().getAngle() / 10) + "");
				nodeFunction.setSelectedItem(ni.getFunction().getName());
				Variable var = ni.getVar(Technology.TECH_TMPVAR);
				if (var != null)
				{
					nodeName.setText((String)var.getObject());
					showNodeName.setSelected(var.getTextDescriptor().isDisplay());
					nodeTextSize.setText(TextUtils.formatDouble(var.getSize().getSize()));
				}
			}
		} else
		{
			nodeAngle.setEnabled(false);
			nodeAngleLabel.setEnabled(false);
			nodeFunction.setEnabled(false);
			nodeFunctionLabel.setEnabled(false);
			nodeName.setEnabled(false);
			nodeNameLabel.setEnabled(false);
			nodeTextSize.setEnabled(false);
			nodeTextSizeLabel.setEnabled(false);
			showNodeName.setEnabled(false);
		}
		changingNodeFields = false;
	}

	/**
	 * Method called when the "add" button is clicked.
	 * Adds an entry to the menu or popup.
	 * @param obj the object to add to the menu entry.
	 */
	private void addToMenu(Object obj)
	{
		if (menuArray[menuSelectedY] == null)
			menuArray[menuSelectedY] = new Object[menuWid];
		Object item = menuArray[menuSelectedY][menuSelectedX];
		if (item == null)
		{
			menuArray[menuSelectedY][menuSelectedX] = obj;
		} else if (item instanceof List)
		{
			List popupItems = (List)item;
			if (!isUniformType(popupItems, obj)) return;
			popupItems.add(obj);
		} else
		{
			List<Object> newList = new ArrayList<Object>();
			newList.add(item);
			if (!isUniformType(newList, obj)) return;
			newList.add(obj);
			menuArray[menuSelectedY][menuSelectedX] = newList;
		}
		menuView.repaint();
		showSelected();
		changed = true;
	}

	/**
	 * Method to tell whether a proposed new object fits with an existing list (all nodes, all arcs, etc).
	 * Arcs cannot be added to node lists, nodes cannot be added to arc lists, and special text cannot
	 * be added to any list.
	 * @param list the list to test.
	 * @param newOne the object being added to the list.
	 * @return true if the object fits in the list.
	 */
	private boolean isUniformType(List list, Object newOne)
	{
		if (newOne instanceof String)
		{
			Job.getUserInterface().showErrorMessage("Must remove everything in the menu before adding 'special text'",
				"Cannot Add");
			return false;
		}
		for(Object oldOne : list)
		{
			if (newOne instanceof ArcProto)
			{
				if (!(oldOne instanceof ArcProto))
				{
					Job.getUserInterface().showErrorMessage("Existing Arc menu can only have other Arcs added to it",
						"Cannot Add");
					return false;
				}
			} else
			{
				if (oldOne instanceof ArcProto)
				{
					Job.getUserInterface().showErrorMessage("Existing Node menu can only have other Nodes added to it",
						"Cannot Add");
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Class to draw the menu.
	 */
	private class MenuView extends JPanel implements MouseListener
	{
		MenuView()
		{
			addMouseListener(this);
		}

		/**
		 * Method to repaint this MenuView.
		 */
		public void paint(Graphics g)
		{
			// clear the area
			Dimension dim = getSize();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, dim.width, dim.height);

			// draw black menu dividers
			g.setColor(Color.BLACK);
			for(int i=0; i<=menuHei; i++)
			{
				int y = (dim.height-1) - (dim.height-1) * i / menuHei;
				g.drawLine(0, y, dim.width-1, y);
			}
			for(int i=0; i<=menuWid; i++)
			{
				int x = (dim.width-1) * i / menuWid;
				g.drawLine(x, 0, x, dim.height-1);
			}

			// draw all of the menu entries
			for (int i = 0; i < menuWid; i++)
			{
				for (int j = 0; j < menuHei; j++)
				{
					int lowX = (dim.width-1) * i / menuWid;
					int lowY = (dim.height-1) - (dim.height-1) * (j+1) / menuHei;
					int highX = (dim.width-1) * (i+1) / menuWid;
					int highY = (dim.height-1) - (dim.height-1) * j / menuHei;
					Object item = (menuArray[j] == null) ? null : menuArray[j][i];
					Color borderColor = null;
					if (item instanceof PrimitiveNode)
					{
						PrimitiveNode np = (PrimitiveNode)item;
						int midY = (lowY + highY) / 2;
						showString(g, "Node", lowX, highX, lowY, midY);
						showString(g, np.getName(), lowX, highX, midY, highY);
						borderColor = Color.BLUE;
					} else if (item instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)item;
						int midY = (lowY + highY) / 2;
						showString(g, "Node", lowX, highX, lowY, midY);
						showString(g, getNodeName(ni), lowX, highX, midY, highY);
						borderColor = Color.BLUE;
					} else if (item instanceof ArcProto)
					{
						ArcProto ap = (ArcProto)item;
						int midY = (lowY + highY) / 2;
						showString(g, "Arc", lowX, highX, lowY, midY);
						showString(g, ap.getName(), lowX, highX, midY, highY);
						borderColor = Color.RED;
					} else if (item instanceof List)
					{
						List popupItems = (List)item;
						for(Object o : popupItems)
						{
							if (o instanceof PrimitiveNode || o instanceof NodeInst) borderColor = Color.BLUE; else
								if (o instanceof ArcProto) borderColor = Color.RED;
						}
						showString(g, "POPUP", lowX, highX, lowY, highY);
					} else if (item instanceof String)
					{
						showString(g, "\"" + (String)item + "\"", lowX, highX, lowY, highY);
					}
					if (borderColor != null)
					{
						g.setColor(borderColor);
						g.drawLine(lowX+1, lowY-1, highX-1, lowY-1);
						g.drawLine(highX-1, lowY-1, highX-1, highY+1);
						g.drawLine(highX-1, highY+1, lowX+1, highY+1);
						g.drawLine(lowX+1, highY+1, lowX+1, lowY-1);
					}
				}
			}

			// highlight the selected menu element
			if (menuSelectedX >= 0 && menuSelectedY >= 0)
			{
				int lowX = (dim.width-1) * menuSelectedX / menuWid;
				int lowY = (dim.height-1) - (dim.height-1) * (menuSelectedY+1) / menuHei;
				int highX = (dim.width-1) * (menuSelectedX+1) / menuWid;
				int highY = (dim.height-1) - (dim.height-1) * menuSelectedY / menuHei;
				g.setColor(Color.GREEN);
				g.drawLine(lowX, lowY, highX, lowY);
				g.drawLine(highX, lowY, highX, highY);
				g.drawLine(highX, highY, lowX, highY);
				g.drawLine(lowX, highY, lowX, lowY);
				g.drawLine(lowX+1, lowY+1, highX-1, lowY+1);
				g.drawLine(highX-1, lowY+1, highX-1, highY-1);
				g.drawLine(highX-1, highY-1, lowX+1, highY-1);
				g.drawLine(lowX+1, highY-1, lowX+1, lowY+1);
			}
		}

		private void showString(Graphics g, String msg, int lowX, int highX, int lowY, int highY)
		{
			g.setColor(Color.BLACK);
			Font font = new Font(User.getDefaultFont(), Font.PLAIN, 9);
			g.setFont(font);
			FontRenderContext frc = new FontRenderContext(null, true, true);
			for(;;)
			{
				GlyphVector gv = font.createGlyphVector(frc, msg);
				LineMetrics lm = font.getLineMetrics(msg, frc);
				double txtHeight = lm.getHeight();
				Rectangle2D rasRect = gv.getLogicalBounds();
				double txtWidth = rasRect.getWidth();
				if (txtWidth <= highX-lowX)
				{
					Graphics2D g2 = (Graphics2D)g;
					g2.drawGlyphVector(gv, (float)(lowX + (highX-lowX - txtWidth)/2),
						(float)(lowY + highY + txtHeight)/2 - lm.getDescent());
					break;
				}
				msg = msg.substring(0, msg.length()-1);
			}
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			Dimension dim = getSize();
			int x = evt.getX() / (dim.width / menuWid);
			int y = (menuHei-1) - (evt.getY() / (dim.height / menuHei));
			if (x < 0 || x >= menuWid || y < 0 || y >= menuHei) return;
			menuSelectedX = x;
			menuSelectedY = y;
			showSelected();
			repaint();
		}

		public void mouseReleased(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        Top = new javax.swing.JPanel();
        nodeListPane = new javax.swing.JScrollPane();
        arcListPane = new javax.swing.JScrollPane();
        menuSize = new javax.swing.JLabel();
        specialListPane = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        lowerRight = new javax.swing.JPanel();
        addRow = new javax.swing.JButton();
        deleteRow = new javax.swing.JButton();
        addColumn = new javax.swing.JButton();
        deleteColumn = new javax.swing.JButton();
        lowerLeft = new javax.swing.JPanel();
        selectedMenuName = new javax.swing.JLabel();
        popupListPane = new javax.swing.JScrollPane();
        nodeAngleLabel = new javax.swing.JLabel();
        nodeFunctionLabel = new javax.swing.JLabel();
        nodeNameLabel = new javax.swing.JLabel();
        nodeTextSizeLabel = new javax.swing.JLabel();
        showNodeName = new javax.swing.JCheckBox();
        nodeTextSize = new javax.swing.JTextField();
        nodeName = new javax.swing.JTextField();
        nodeFunction = new javax.swing.JComboBox();
        nodeAngle = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 10));

        setTitle("Component Menu");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        Top.setLayout(new java.awt.GridBagLayout());

        nodeListPane.setPreferredSize(new java.awt.Dimension(200, 200));
        nodeListPane.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        Top.add(nodeListPane, gridBagConstraints);

        arcListPane.setPreferredSize(new java.awt.Dimension(200, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        Top.add(arcListPane, gridBagConstraints);

        menuSize.setText("Menu");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        Top.add(menuSize, gridBagConstraints);

        specialListPane.setOpaque(false);
        specialListPane.setPreferredSize(new java.awt.Dimension(200, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        Top.add(specialListPane, gridBagConstraints);

        jLabel2.setText("Nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        Top.add(jLabel2, gridBagConstraints);

        jLabel3.setText("Arcs:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        Top.add(jLabel3, gridBagConstraints);

        jLabel4.setText("Special:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        Top.add(jLabel4, gridBagConstraints);

        addButton.setText("<< Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(addButton, gridBagConstraints);

        removeButton.setText("Remove");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(removeButton, gridBagConstraints);

        lowerRight.setLayout(new java.awt.GridBagLayout());

        addRow.setText("Add Row Below Current");
        addRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRowActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        lowerRight.add(addRow, gridBagConstraints);

        deleteRow.setText("Delete Row With Current");
        deleteRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteRowActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        lowerRight.add(deleteRow, gridBagConstraints);

        addColumn.setText("Add Column to Right of Current");
        addColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addColumnActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        lowerRight.add(addColumn, gridBagConstraints);

        deleteColumn.setText("Delete Column With Current");
        deleteColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteColumnActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        lowerRight.add(deleteColumn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        Top.add(lowerRight, gridBagConstraints);

        lowerLeft.setLayout(new java.awt.GridBagLayout());

        selectedMenuName.setText("selected menu");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        lowerLeft.add(selectedMenuName, gridBagConstraints);

        popupListPane.setPreferredSize(new java.awt.Dimension(200, 70));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        lowerLeft.add(popupListPane, gridBagConstraints);

        nodeAngleLabel.setText("Angle:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        lowerLeft.add(nodeAngleLabel, gridBagConstraints);

        nodeFunctionLabel.setText("Function:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        lowerLeft.add(nodeFunctionLabel, gridBagConstraints);

        nodeNameLabel.setText("Label:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        lowerLeft.add(nodeNameLabel, gridBagConstraints);

        nodeTextSizeLabel.setText("Label Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        lowerLeft.add(nodeTextSizeLabel, gridBagConstraints);

        showNodeName.setText("Show Label in Menu");
        showNodeName.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        showNodeName.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        lowerLeft.add(showNodeName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        lowerLeft.add(nodeTextSize, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        lowerLeft.add(nodeName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        lowerLeft.add(nodeFunction, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        lowerLeft.add(nodeAngle, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        Top.add(lowerLeft, gridBagConstraints);

        getContentPane().add(Top, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void deleteColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteColumnActionPerformed
		if (menuWid <= 1)
		{
			Job.getUserInterface().showErrorMessage("There must be at least one column...cannot delete the last one",
				"Cannot Remove Column");
			return;
		}
		for(int y=0; y<menuHei; y++)
		{
			Object [] newRow = new Object[menuWid-1];
			int fill = 0;
			for(int x=0; x<menuWid; x++)
			{
				if (x == menuSelectedX) continue;
				newRow[fill++] = menuArray[y][x];
			}
			menuArray[y] = newRow;
		}
		menuWid--;
		if (menuSelectedX >= menuWid) menuSelectedX--;
		menuView.repaint();
		showSelected();
		showMenuSize();
		changed = true;
	}//GEN-LAST:event_deleteColumnActionPerformed

    private void addColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addColumnActionPerformed
		for(int y=0; y<menuHei; y++)
		{
			Object [] newRow = new Object[menuWid+1];
			int fill = 0;
			for(int x=0; x<menuWid; x++)
			{
				newRow[fill++] = menuArray[y][x];
				if (x == menuSelectedX) newRow[fill++] = null;
			}
			menuArray[y] = newRow;
		}
		menuWid++;
		menuSelectedX++;
		menuView.repaint();
		showSelected();
		showMenuSize();
		changed = true;
	}//GEN-LAST:event_addColumnActionPerformed

    private void deleteRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteRowActionPerformed
		if (menuHei <= 1)
		{
			Job.getUserInterface().showErrorMessage("There must be at least one row...cannot delete the last one",
				"Cannot Remove Row");
			return;
		}
		Object [][] newMenu = new Object[menuHei-1][];
		int fill = 0;
		for(int y=0; y<menuHei; y++)
		{
			if (y == menuSelectedY) continue;
			newMenu[fill++] = menuArray[y];
		}
		menuArray = newMenu;
		menuHei--;
		if (menuSelectedY >= menuHei) menuSelectedY--;
		menuView.repaint();
		showSelected();
		showMenuSize();
		changed = true;
	}//GEN-LAST:event_deleteRowActionPerformed

    private void addRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRowActionPerformed
		Object [][] newMenu = new Object[menuHei+1][];
		int fill = 0;
		for(int y=0; y<menuHei; y++)
		{
			if (y == menuSelectedY) newMenu[fill++] = new Object[menuWid];
			newMenu[fill++] = menuArray[y];
		}
		menuArray = newMenu;
		menuHei++;
		menuSelectedY++;
		menuView.repaint();
		showSelected();
		showMenuSize();
		changed = true;
	}//GEN-LAST:event_addRowActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
		if (menuArray[menuSelectedY] == null)
			menuArray[menuSelectedY] = new Object[menuWid];
		Object item = menuArray[menuSelectedY][menuSelectedX];
		if (item == null) return;
		if (item instanceof List)
		{
			List popupItems = (List)item;
			int index = listPopup.getSelectedIndex();
			if (index < 0)
			{
				Job.getUserInterface().showErrorMessage("Must first select the popup item to be removed from the list",
					"Cannot Remove");
				return;
			}
			popupItems.remove(index);
			if (popupItems.size() == 1) menuArray[menuSelectedY][menuSelectedX] = popupItems.get(0);
		} else
		{
			menuArray[menuSelectedY][menuSelectedX] = null;
		}
		menuView.repaint();
		showSelected();
		changed = true;
	}//GEN-LAST:event_removeButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
		switch (lastListSelected)
		{
			case 0:	// add a node
				String nodeName = (String)listNodes.getSelectedValue();
				PrimitiveNode pnp = tech.findNodeProto(nodeName);
				addToMenu(pnp);
				break;
			case 1: // add an arc
				String arcName = (String)listArcs.getSelectedValue();
				ArcProto ap = tech.findArcProto(arcName);
				addToMenu(ap);
				break;
			case 2:	// add a special text
				String specialName = (String)listSpecials.getSelectedValue();
				addToMenu(specialName);
				break;
		}
	}//GEN-LAST:event_addButtonActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Top;
    private javax.swing.JButton addButton;
    private javax.swing.JButton addColumn;
    private javax.swing.JButton addRow;
    private javax.swing.JScrollPane arcListPane;
    private javax.swing.JButton deleteColumn;
    private javax.swing.JButton deleteRow;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel lowerLeft;
    private javax.swing.JPanel lowerRight;
    private javax.swing.JLabel menuSize;
    private javax.swing.JTextField nodeAngle;
    private javax.swing.JLabel nodeAngleLabel;
    private javax.swing.JComboBox nodeFunction;
    private javax.swing.JLabel nodeFunctionLabel;
    private javax.swing.JScrollPane nodeListPane;
    private javax.swing.JTextField nodeName;
    private javax.swing.JLabel nodeNameLabel;
    private javax.swing.JTextField nodeTextSize;
    private javax.swing.JLabel nodeTextSizeLabel;
    private javax.swing.JScrollPane popupListPane;
    private javax.swing.JButton removeButton;
    private javax.swing.JLabel selectedMenuName;
    private javax.swing.JCheckBox showNodeName;
    private javax.swing.JScrollPane specialListPane;
    // End of variables declaration//GEN-END:variables

}
