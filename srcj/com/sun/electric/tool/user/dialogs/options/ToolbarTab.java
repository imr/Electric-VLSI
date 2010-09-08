/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolbarTab.java
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuItem;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.ToolBar.EToolBarButton;
import com.sun.electric.util.TextUtils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Class to handle the "Toolbar" tab of the Preferences dialog.
 */
public class ToolbarTab extends PreferencePanel implements TreeSelectionListener
{
	private CommandTree commandsTree;
	private List<DraggableToolbarEntry> sampleToolbarComponents;
	private Map<String,EToolBarButton> knownButtons;
	private Map<String,EMenuItem> menuEntries;
	private Map<String,String> commandToIconMap;
	private MenuTreeNode currentTreeNode;
	private PreferencesFrame trueParent;
	private static ImageIcon menuIcon = Resources.getResource(ToolBar.class, "ButtonMenu.gif");
	private static ImageIcon trashIcon = Resources.getResource(ToolBar.class, "IconTrash.gif");
	private static ImageIcon separatorIcon = Resources.getResource(ToolBar.class, "IconSeparator.gif");
	private static ImageIcon separatorButton = Resources.getResource(ToolBar.class, "ButtonSeparator.gif");

	/** Creates new form ToolbarTab */
	public ToolbarTab(Frame parent, boolean modal, PreferencesFrame trueParent)
	{
		super(parent, modal);
		this.trueParent = trueParent;
		initComponents();

		// initialize the separator icon (and make it draggable)
		separatorLabel.setIcon(separatorIcon);
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(separatorLabel, DnDConstants.ACTION_LINK, new SeparatorDrag());

		// initialize the trash icon (and make it droppable)
		trashLabel.setIcon(trashIcon);
		TrashDropTarget dropTarget = new TrashDropTarget();
		new DropTarget(trashLabel, DnDConstants.ACTION_MOVE, dropTarget, true);
	}

	/** return the panel to use for user preferences. */
	public JPanel getUserPreferencesPanel() { return toolbar; }

	/** return the name of this preferences tab. */
	public String getName() { return "Toolbar"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Toolbar tab.
	 */
	public void init()
	{
		// initialize the known buttons that are in the toolbar
		knownButtons = new HashMap<String,EToolBarButton>();
		List<ToolBar.EToolBarButton> allButtons = ToolBar.getAllButtons();
		for (ToolBar.EToolBarButton b : allButtons)
			knownButtons.put(b.getMenuName() + ":" + b.getText().replace("_", ""), b);

		// build the command tree
		commandsTree = new CommandTree();
		buildCommandsTree();

		// add in new buttons that have custom icons
		commandToIconMap = ToolBar.getCommandToIconMap();
		for(String command : commandToIconMap.keySet())
		{
			String fileName = commandToIconMap.get(command);
			ToolBar.EToolBarButton but = knownButtons.get(command);
			if (but == null)
			{
				int lastColon = command.lastIndexOf(':');
				String menuName = (lastColon < 0) ? "" : command.substring(0, lastColon);
				String commandName = command.substring(lastColon+1);
				EMenuItem mi = menuEntries.get(command);
				if (mi == null) continue;
				but = new ToolBar.EToolBarGeneralMenuButton(commandName, "ButtonUnknown", menuName, mi);
				knownButtons.put(command, but);
			}
			ImageIcon icon = ToolBar.getProperSizeIcon(fileName);
			but.setIcon(icon, fileName);
		}

		// finish initializing the command tree
		commandsTree.setCellRenderer(new MyRenderer());
		commandsPane.setViewportView(commandsTree);

		// make sure all button icons exist in the menus
		for(String iconCommand : knownButtons.keySet())
		{
			if (menuEntries.get(iconCommand) == null)
				System.out.println("WARNING: Icon mapped to command '" + iconCommand + "' which does not exist");
		}

		// construct the toolbar
		currentToolbar.setLayout(new GridBagLayout());
		EToolBarButton [] currentButtons = ToolBar.getToolbarButtons();
		sampleToolbarComponents = new ArrayList<DraggableToolbarEntry>();
		for (int i=0; i<currentButtons.length; i++)
		{
			EToolBarButton b = currentButtons[i];
			DraggableToolbarEntry j = new DraggableToolbarEntry(i, b);
			sampleToolbarComponents.add(j);
		}
		buildSampleToolbar();
	}

	/**
	 * Method called when the "OK" button is hit.
	 * Updates any changed fields in the Toolbar tab.
	 */
	public void term()
	{
		ToolBar.setCommandToIconMap(commandToIconMap);
		EToolBarButton [] newButtons = new EToolBarButton[sampleToolbarComponents.size()];
		for(int i=0; i<sampleToolbarComponents.size(); i++)
			newButtons[i] = sampleToolbarComponents.get(i).getToolbarButton();
		ToolBar.setToolbarButtons(newButtons);
	}

	/**
	 * Method called when the factory reset is requested for just this panel.
	 * @return true if the panel can be reset "in place" without redisplay.
	 */
	public boolean resetThis()
	{
		sampleToolbarComponents.clear();
		EToolBarButton [] factorySet = ToolBar.getFactoryButtons();
		for(int i=0; i<factorySet.length; i++)
			sampleToolbarComponents.add(new DraggableToolbarEntry(i, factorySet[i]));
		buildSampleToolbar();
		trueParent.pack();
		return true;
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		EToolBarButton [] fButs = ToolBar.getFactoryButtons();
		EToolBarButton [] buts = ToolBar.getToolbarButtons();
		boolean same = fButs.equals(buts);
		if (!same)
			ToolBar.setToolbarButtons(ToolBar.getFactoryButtons());
	}

	private void buildSampleToolbar()
	{
		currentToolbar.removeAll();
		for (int i=0; i<sampleToolbarComponents.size(); i++)
		{
			DraggableToolbarEntry j = sampleToolbarComponents.get(i);
			j.setIndex(i);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = i;   gbc.gridy = 0;
			currentToolbar.add(j, gbc);
		}
	}

	private void dropButton(String droppedButton)
	{
		if (droppedButton.startsWith("#"))
		{
			int index = TextUtils.atoi(droppedButton.substring(1));
			sampleToolbarComponents.remove(index);
			buildSampleToolbar();
			trueParent.pack();
		}
	}

	private void droppedAt(int index, String what, boolean before)
	{
		if (!before) index++;
		if (what.startsWith("#"))
		{
			// a rearrangement of buttons
			int startLoc = TextUtils.atoi(what.substring(1));
			DraggableToolbarEntry j = sampleToolbarComponents.get(startLoc);
			sampleToolbarComponents.remove(startLoc);
			if (startLoc < index) index--;
			sampleToolbarComponents.add(index, j);
		} else
		{
			if (what.equals("SEPARATOR"))
			{
				// inserting separator
				DraggableToolbarEntry j = new DraggableToolbarEntry(0, null);
				sampleToolbarComponents.add(index, j);
			} else
			{
				// inserting new command
				EToolBarButton but = knownButtons.get(what);
				if (but == null)
				{
					int lastColon = what.lastIndexOf(':');
					String menuName = (lastColon < 0) ? "" : what.substring(0, lastColon);
					String commandName = what.substring(lastColon+1);
					EMenuItem mi = menuEntries.get(what);
					but = new ToolBar.EToolBarGeneralMenuButton(commandName, "ButtonUnknown", menuName, mi);
					knownButtons.put(what, but);
				}
				DraggableToolbarEntry j = new DraggableToolbarEntry(0, but);
				sampleToolbarComponents.add(index, j);
			}
		}

		buildSampleToolbar();
		trueParent.pack();
	}

	/**
	 * Called when selection of Node in tree changes.
	 * It updates the list box to reflect the current tree selection.
	 */
	public void valueChanged(TreeSelectionEvent e)
	{
		currentTreeNode = null;
		TreePath path = e.getPath();
		if (path == null) return;
		Object obj = path.getLastPathComponent();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
		Object n = node.getUserObject();
		if (!(n instanceof MenuTreeNode)) return;
		MenuTreeNode mtn = (MenuTreeNode)n;
		if (mtn.menuName.endsWith("--")) return;
		currentTreeNode = mtn;
	}

	private void attachImageToCommand()
	{
		if (currentTreeNode == null)
		{
			Job.getUserInterface().showErrorMessage("Must select a command from the 'Commands' tree first", "Cannot Set Image");
			return;
		}
		String fileName = OpenFile.chooseInputFile(FileType.ANY, "Image to attach to command");
		if (fileName == null) return;
		ImageIcon icon = ToolBar.getProperSizeIcon(fileName);
		if (icon != null)
		{
			// find the button associated with this command and set the icon
			String what = currentTreeNode.menuName;
			EToolBarButton but = knownButtons.get(what);
			if (but == null)
			{
				int lastColon = what.lastIndexOf(':');
				String menuName = (lastColon < 0) ? "" : what.substring(0, lastColon);
				String commandName = what.substring(lastColon+1);
				EMenuItem mi = menuEntries.get(what);
				but = new ToolBar.EToolBarGeneralMenuButton(commandName, "ButtonUnknown", menuName, mi);
				knownButtons.put(what, but);
			}
			but.setIcon(icon, fileName);
			commandToIconMap.put(what, fileName);
			commandsTree.updateUI();
		}
	}

	/** Build tree of menu commands */
	private void buildCommandsTree()
	{
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
		menuEntries = new HashMap<String,EMenuItem>();

		TopLevel top = TopLevel.getCurrentJFrame();
		if (top == null || top.getEMenuBar() == null) return;

		// convert menuBar to tree
		for (EMenuItem menu: top.getEMenuBar().getItems())
		{
			DefaultMutableTreeNode menuNode = new DefaultMutableTreeNode(new MenuTreeNode(menu, menu.getText()));
			rootNode.add(menuNode);
			addMenu(menuNode, (EMenu)menu, menu.getText());
		}
		EMenu hiddenMenu = top.getEMenuBar().getHiddenMenu();
		if (hiddenMenu != null)
		{
			DefaultMutableTreeNode menuNode = new DefaultMutableTreeNode(new MenuTreeNode(hiddenMenu, hiddenMenu.getText()));
			rootNode.add(menuNode);
			addMenu(menuNode, hiddenMenu, hiddenMenu.getText());
		}

		commandsTree.setModel(new DefaultTreeModel(rootNode));
		commandsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		commandsTree.setRootVisible(false);
		commandsTree.setShowsRootHandles(true);
		commandsTree.addTreeSelectionListener(this);
	}

	/** Adds menu items to parentNode, which represents Menu menu. */
	private void addMenu(DefaultMutableTreeNode parentNode, EMenu menu, String menuName)
	{
		for (EMenuItem menuItem : menu.getItems())
		{
			String menuItemName = menuName + ":" + menuItem.getText();
			DefaultMutableTreeNode menuItemNode = new DefaultMutableTreeNode(new MenuTreeNode(menuItem, menuItemName));
			parentNode.add(menuItemNode);
			if (menuItem instanceof EMenu)
			{
				addMenu(menuItemNode, (EMenu)menuItem, menuItemName);
			} else
			{
				menuEntries.put(menuItemName, menuItem);
			}
		}
	}

	/**
	 * Class to encapsulate a tree node for displaying icons bound to commands.
	 * The toString() method is overridden to show the key binding next to the
	 * command name.  This class encapsulates both JMenuItem and Menu, note
	 * that both extend JMenuItem.
	 */
	private class MenuTreeNode
	{
		private EMenuItem menuItem;
		private String menuName;

		MenuTreeNode(EMenuItem menuItem, String menuName)
		{
			this.menuItem = menuItem;
			this.menuName = menuName;
		}

		public EMenuItem getMenuItem() { return menuItem; }

		/**
		 * Convert to String to show on dialog tree
		 */
		public String toString()
		{
			if (menuItem != EMenuItem.SEPARATOR)
			{
				StringBuffer buf = new StringBuffer(menuItem.getDescription());
				return buf.toString();
			}
			return "---------------";			   // separator
		}
	}

	private class MyRenderer extends DefaultTreeCellRenderer
	{
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
			boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			if (!(value instanceof DefaultMutableTreeNode)) return this;
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			if (node.getChildCount() > 0)
			{
				setIcon(menuIcon);
			} else
			{
				Object nodeInfo = node.getUserObject();
				if (nodeInfo instanceof MenuTreeNode)
				{
					MenuTreeNode mtn = (MenuTreeNode)nodeInfo;
					if (mtn.menuItem == EMenuItem.SEPARATOR)
					{
						setIcon(null);
					} else
					{
						EToolBarButton but = knownButtons.get(mtn.menuName);
						if (but != null) setIcon(but.getIcon()); else
						{
							setIcon(ToolBar.getUnknownIcon());
						}
					}
				}
			}
			return this;
		}
	}

	private class CommandTree extends JTree implements DragSourceListener
	{
		CommandTree()
		{
			setTransferHandler(new MyTransferHandler(getTransferHandler()));
			setDragEnabled(true);
		}

		public void dragEnter(DragSourceDragEvent e) {}
		public void dragOver(DragSourceDragEvent e) {}
		public void dragExit(DragSourceEvent e) {}
		public void dragDropEnd(DragSourceDropEvent e) {}
		public void dropActionChanged (DragSourceDragEvent e) {}

		/**
		 * Class to start drag of a command in the tree.
		 */
		private class MyTransferHandler extends TransferHandler
		{
			private TransferHandler real;

			MyTransferHandler(TransferHandler real)
			{
				this.real = real;
			}

			protected Transferable createTransferable(JComponent c)
			{
				if (currentTreeNode == null) return null;
				Transferable transferable = new StringSelection("TOOLBARBUTTON " + currentTreeNode.menuName);
				return transferable;
			}

			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) { return real.canImport(comp, transferFlavors); }
			public void exportAsDrag(JComponent comp, InputEvent e, int action)  { real.exportAsDrag(comp, e, action); }
			protected void exportDone(JComponent source, Transferable data, int action)  {  }
			public void exportToClipboard(JComponent comp, Clipboard clip, int action)  { real.exportToClipboard(comp, clip, action); }
			public int getSourceActions(JComponent c)  { return real.getSourceActions(c); }
			public Icon getVisualRepresentation(Transferable t)  { return real.getVisualRepresentation(t); }
			public boolean importData(JComponent comp, Transferable t) { return real.importData(comp, t); }
		}
	}

	/**
	 * Class for defining toolbar entries that are draggable and droppable.
	 */
	private class DraggableToolbarEntry extends JLabel implements DragGestureListener, DragSourceListener
	{
		private DragSource dragSource;
		private int editIndex;
		private EToolBarButton toolbarButton;

		public DraggableToolbarEntry(int editIndex, EToolBarButton toolbarButton)
		{
			this.editIndex = editIndex;
			this.toolbarButton = toolbarButton;
			ImageIcon icon = (toolbarButton == null) ? separatorButton : toolbarButton.getIcon();
			int width = icon.getIconWidth();
			int height = icon.getIconHeight();
			BufferedImage originalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics g = originalImage.getGraphics();
			g.drawImage(icon.getImage(), 0, 0, null);
			Raster originalData = originalImage.getData();

			int border = 4;
			BufferedImage biggerImage = new BufferedImage(width+border*2, height+border*2, BufferedImage.TYPE_INT_ARGB);
			WritableRaster biggerRaster = biggerImage.getRaster();
			int [] sArray = new int[4];
			for(int y=0; y<height; y++)
			{
				for(int x=0; x<width; x++)
				{
					originalData.getPixel(x, y, sArray);
					biggerRaster.setPixel(x+border, y+border, sArray);
				}
			}
			setIcon(new ImageIcon(biggerImage));
			if (toolbarButton != null) setToolTipText(toolbarButton.getText());
			dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);

			ToolBarDropTarget dropTarget = new ToolBarDropTarget();
			new DropTarget(this, DnDConstants.ACTION_MOVE, dropTarget, true);
		}

		public void setIndex(int i) { editIndex = i; }

		public EToolBarButton getToolbarButton() { return toolbarButton; }

		public void dragGestureRecognized(DragGestureEvent e)
		{
			// make the Transferable Object
			Transferable transferable = new StringSelection("TOOLBARBUTTON #" + editIndex);

			// begin the drag
			dragSource.startDrag(e, DragSource.DefaultMoveDrop, transferable, this);
		}

		public void dragEnter(DragSourceDragEvent e) {}
		public void dragOver(DragSourceDragEvent e) {}
		public void dragExit(DragSourceEvent e) {}
		public void dragDropEnd(DragSourceDropEvent e) {}
		public void dropActionChanged (DragSourceDragEvent e) {}

		/**
		 * Class for catching drags onto the toolbar.
		 * These drags come from elsewhere in the toolbar (for rearrangement)
		 * or from the toolbar preferences panel.
		 */
		private class ToolBarDropTarget implements DropTargetListener
		{
			private JLabel lastButtonDrawn = null;

			public void dragEnter(DropTargetDragEvent e)
			{
				dragAction(e);
			}

			public void dragOver(DropTargetDragEvent e)
			{
				if (dragAction(e)) return;

				DropTarget dt = (DropTarget)e.getSource();
				if (dt.getComponent() instanceof JLabel)
				{
					JLabel but = (JLabel)dt.getComponent();

					// erase former highlighting
					eraseDragImage(dt);

					// highlight what the drag is over
					Graphics2D g2 = (Graphics2D)but.getGraphics();
					g2.setColor(Color.RED);
					int x = 0;
					if (e.getLocation().x > but.getWidth()/2) x = but.getWidth()-2;
					g2.drawRect(x, 0, 1, but.getHeight());
					lastButtonDrawn = but;
				}
			}

			public void dropActionChanged(DropTargetDragEvent e)
			{
				dragAction(e);
			}

			public void dragExit(DropTargetEvent e)
			{
				eraseDragImage((DropTarget)e.getSource());
			}

			public void drop(DropTargetDropEvent dtde)
			{
				dtde.acceptDrop(DnDConstants.ACTION_MOVE);

				// erase former highlighting
				eraseDragImage((DropTarget)dtde.getSource());

				// get the files that were dragged
				String droppedButton = getDraggedObject(dtde.getTransferable(), dtde.getCurrentDataFlavors());
				if (droppedButton != null)
				{
					// see what the drop is over
					DropTarget dt = (DropTarget)dtde.getSource();
					Component but = dt.getComponent();
					if (but != null)
					{
						// find index in menu
						int index = -1;
						for(int i=0; i<but.getParent().getComponentCount(); i++)
							if (but.getParent().getComponent(i) == but) { index = i;   break; }
						boolean before = true;
						if (dtde.getLocation().x > but.getWidth()/2) before = false;
						if (index >= 0)
							droppedAt(index, droppedButton, before);
						dtde.dropComplete(true);
						return;
					}
				}
				dtde.dropComplete(false);
			}

			/**
			 * Method to analyze the drag.
			 * @param e
			 * @return true if the drag is to be rejected.
			 */
			private boolean dragAction(DropTargetDragEvent e)
			{
				String droppedButton = getDraggedObject(e.getTransferable(), e.getCurrentDataFlavors());
				if (droppedButton != null)
				{
					e.acceptDrag(e.getDropAction());
					return false;
				}
				e.rejectDrag();
				return true;
			}

			/**
			 * Method to get the name of the button that is being dragged.
			 * @param tra the Transferable with this information.
			 * @param flavors an array of flavors of the dragged information.
			 * @return the name of the button that is being dragged (null on error).
			 */
			private String getDraggedObject(Transferable tra, DataFlavor [] flavors)
			{
				if (flavors.length <= 0) return null;

				// text dragging is name of button
				if (flavors[0].isFlavorTextType())
				{
					Object obj = null;
					try
					{
						obj = tra.getTransferData(flavors[0]);
					} catch (Exception ex) {}
					if (obj instanceof String)
					{
						String buttonName = (String)obj;
						if (buttonName.startsWith("TOOLBARBUTTON ")) return buttonName.substring(14);
					}
				}
				return null;
			}

			private void eraseDragImage(DropTarget dt)
			{
				if (lastButtonDrawn == null) return;
				Rectangle pathBounds = new Rectangle(0, 0, lastButtonDrawn.getWidth(), lastButtonDrawn.getHeight());
				lastButtonDrawn.paintImmediately(pathBounds);
				lastButtonDrawn = null;
			}
		}
	}

	/**
	 * Class to start a drag of the separator icon.
	 */
	private class SeparatorDrag implements DragSourceListener, DragGestureListener
	{
		/**
		 * Method to start a drag from the separator icon.
		 */
		public void dragGestureRecognized(DragGestureEvent e)
		{
			Transferable transferable = new StringSelection("TOOLBARBUTTON SEPARATOR");
			e.startDrag(null, separatorIcon.getImage(), new Point(0, 0), transferable, this);
		}

		public void dragEnter(DragSourceDragEvent e) {}
		public void dragOver(DragSourceDragEvent e) {}
		public void dragExit(DragSourceEvent e) {}
		public void dragDropEnd(DragSourceDropEvent e) {}
		public void dropActionChanged (DragSourceDragEvent e) {}
	}

	/**
	 * Class for catching drags onto the trash.
	 * These drags come from elsewhere in the toolbar (for rearrangement).
	 */
	private class TrashDropTarget implements DropTargetListener
	{
		private boolean trashHighlighted = false;

		public void dragEnter(DropTargetDragEvent e)
		{
			dragAction(e);
		}

		public void dragOver(DropTargetDragEvent e)
		{
			if (dragAction(e)) return;

			DropTarget dt = (DropTarget)e.getSource();
			if (dt.getComponent() == trashLabel)
			{
				// erase former highlighting
				eraseDragImage(dt);

				// highlight what the drag is over
				Graphics2D g2 = (Graphics2D)trashLabel.getGraphics();
				g2.setColor(Color.RED);
				g2.drawRect(0, 0, trashLabel.getWidth()-1, trashLabel.getHeight()-1);
				trashHighlighted = true;
			}
		}

		public void dropActionChanged(DropTargetDragEvent e)
		{
			dragAction(e);
		}

		public void dragExit(DropTargetEvent e)
		{
			eraseDragImage((DropTarget)e.getSource());
		}

		public void drop(DropTargetDropEvent dtde)
		{
			dtde.acceptDrop(DnDConstants.ACTION_MOVE);

			// erase former highlighting
			eraseDragImage((DropTarget)dtde.getSource());

			// get the files that were dragged
			String droppedButton = getDraggedObject(dtde.getTransferable(), dtde.getCurrentDataFlavors());
			if (droppedButton != null)
			{
				// see what the drop is over
				DropTarget dt = (DropTarget)dtde.getSource();
				Component but = dt.getComponent();
				if (but != null)
				{
					dropButton(droppedButton);
					dtde.dropComplete(true);
					return;
				}
			}
			dtde.dropComplete(false);
		}

		/**
		 * Method to analyze the drag.
		 * @param e
		 * @return true if the drag is to be rejected.
		 */
		private boolean dragAction(DropTargetDragEvent e)
		{
			String droppedButton = getDraggedObject(e.getTransferable(), e.getCurrentDataFlavors());
			if (droppedButton != null)
			{
				e.acceptDrag(e.getDropAction());
				return false;
			}
			e.rejectDrag();
			return true;
		}

		/**
		 * Method to get the name of the button that is being dragged.
		 * @param tra the Transferable with this information.
		 * @param flavors an array of flavors of the dragged information.
		 * @return the name of the button that is being dragged (null on error).
		 */
		private String getDraggedObject(Transferable tra, DataFlavor [] flavors)
		{
			if (flavors.length <= 0) return null;

			// text dragging is name of button
			if (flavors[0].isFlavorTextType())
			{
				Object obj = null;
				try
				{
					obj = tra.getTransferData(flavors[0]);
				} catch (Exception ex) {}
				if (obj instanceof String)
				{
					String buttonName = (String)obj;
					if (buttonName.startsWith("TOOLBARBUTTON #"))
					{
						String bn = buttonName.substring(14);
						return bn;
					}
				}
			}
			return null;
		}

		private void eraseDragImage(DropTarget dt)
		{
			if (!trashHighlighted) return;
			Rectangle pathBounds = new Rectangle(0, 0, trashLabel.getWidth(), trashLabel.getHeight());
			trashLabel.paintImmediately(pathBounds);
			trashHighlighted = false;
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        toolbar = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        currentToolbar = new javax.swing.JPanel();
        commandsPane = new javax.swing.JScrollPane();
        trashLabel = new javax.swing.JLabel();
        separatorLabel = new javax.swing.JLabel();
        attachImage = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        toolbar.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("To add commands, drag them from the \"Commands\" to the \"Toolbar\".");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        toolbar.add(jLabel1, gridBagConstraints);

        jLabel2.setText("To remove commands, drag them from the \"Toolbar\" to the trash.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        toolbar.add(jLabel2, gridBagConstraints);

        jLabel3.setText("To rearrange icons, drag them around the \"Toolbar\".");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        toolbar.add(jLabel3, gridBagConstraints);

        jLabel4.setText("To add separators, drag the \"Sep\" icon to the \"Toolbar\".");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        toolbar.add(jLabel4, gridBagConstraints);

        currentToolbar.setBorder(javax.swing.BorderFactory.createTitledBorder("Toolbar:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        toolbar.add(currentToolbar, gridBagConstraints);

        commandsPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Commands:"));
        commandsPane.setMinimumSize(new java.awt.Dimension(32, 150));
        commandsPane.setPreferredSize(new java.awt.Dimension(32, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        toolbar.add(commandsPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 10);
        toolbar.add(trashLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 20);
        toolbar.add(separatorLabel, gridBagConstraints);

        attachImage.setText("Attach Image to Command...");
        attachImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                attachImageActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 8);
        toolbar.add(attachImage, gridBagConstraints);

        getContentPane().add(toolbar, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void attachImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_attachImageActionPerformed
    	attachImageToCommand();
    }//GEN-LAST:event_attachImageActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton attachImage;
    private javax.swing.JScrollPane commandsPane;
    private javax.swing.JPanel currentToolbar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel separatorLabel;
    private javax.swing.JPanel toolbar;
    private javax.swing.JLabel trashLabel;
    // End of variables declaration//GEN-END:variables

}
