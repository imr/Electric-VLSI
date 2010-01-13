/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolBar.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuBar;
import com.sun.electric.tool.user.menus.EMenuItem;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.MenuCommands;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * This class manages the Electric toolbar.
 */
public class ToolBar extends JToolBar
{
	private static ImageIcon unknownIcon = Resources.getResource(ToolBar.class, "ButtonUnknown.gif");
	private static Pref toolbarOrderPref = Pref.makeStringPref("ToolbarOrder", User.getUserTool().prefs, "");
	private static Pref toolbarFilesPref = Pref.makeStringPref("ToolbarIconFiles", User.getUserTool().prefs, "");
	private static EToolBarButton[] currentToolbarButtons;
	private static List<EToolBarButton> allButtons = new ArrayList<EToolBarButton>();
	private static Map<String,String> commandToIconMap;

	/**
	 * Method to create the toolbar.
	 */
	public static ToolBar createToolBar() { return new ToolBar(); }

	private ToolBar()
	{
		setFloatable(true);
		setRollover(true);
		redoToolbar();
		setFocusable(false);
	}

	/**
	 * Method to return the "factory default" set of toolbar buttons.
	 * @return an array of default toolbar buttons.
	 */
	public static EToolBarButton[] getFactoryButtons()
	{
		// use built-in default
		EToolBarButton[] buttons = new EToolBarButton[] {
			openLibraryCommand,				// File:Open Library...
			saveLibraryCommand,				// File:Save All Libraries
			null,
			clickZoomWireCommand,			// Edit:Modes:Edit:Click/Zoom/Wire
			panCommand,						// Edit:Modes:Edit:Toggle Pan
			zoomCommand,					// Edit:Modes:Edit:Toggle Zoom
			outlineCommand,					// Edit:Modes:Edit:Toggle Outline Edit
			measureCommand,					// Edit:Modes:Edit:Toggle Measure Distance
			null,
			gridLarger,
			gridSmaller,
			null,
			selectObjectsCommand,			// Edit:Modes:Select:Select Objects
			selectAreaCommand,				// Edit:Modes:Select:Select Area
			null,
			toggleSelectSpecialCommand,		// Edit:Modes:Select:Toggle Special Select
			null,
			preferencesCommand,				// File:Preferences...
			null,
			undoCommand,					// Edit:Undo
			redoCommand,					// Edit:Redo
			null,
			goBackButtonStatic,				// Window:Go To Previous Focus
			goForwardButtonStatic,			// Window:Go To Next Focus
			null,
			expandOneLevelCommand,			// Cell:Expand Cell Instances:One Level Down
			unexpandOneLevelCommand			// Cell:Unexpand Cell Instances:One Level Up
		};
		return buttons;
	}

	/**
	 * Method to return a list of all known toolbar buttons.
	 * @return a list of all known toolbar buttons.
	 */
	public static List<EToolBarButton> getAllButtons()
	{
		if (allButtons.size() == 0)
		{
			allButtons.add(openLibraryCommand);
			allButtons.add(saveLibraryCommand);
			allButtons.add(clickZoomWireCommand);
			allButtons.add(panCommand);
			allButtons.add(zoomCommand);
			allButtons.add(outlineCommand);
			allButtons.add(measureCommand);
			allButtons.add(gridLarger);
			allButtons.add(gridSmaller);
			allButtons.add(selectObjectsCommand);
			allButtons.add(selectAreaCommand);
			allButtons.add(toggleSelectSpecialCommand);
			allButtons.add(preferencesCommand);
			allButtons.add(undoCommand);
			allButtons.add(redoCommand);
			allButtons.add(goBackButtonStatic);
			allButtons.add(goForwardButtonStatic);
			allButtons.add(expandOneLevelCommand);
			allButtons.add(unexpandOneLevelCommand);
		}
		return allButtons;
	}

	/**
	 * Method to return all of the buttons in the toolbar.
	 * @return an array of buttons in the toolbar.
	 */
	public static EToolBarButton[] getToolbarButtons()
	{
		if (currentToolbarButtons == null)
			currentToolbarButtons = getDefaultToolbarButtons();
		return currentToolbarButtons;
	}

	/**
	 * Method to return the icon to use when no icon can be found.
	 * @return the default icon for the toolbar.
	 */
	public static ImageIcon getUnknownIcon() { return unknownIcon; }

	/**
	 * Method to change the order of buttons in the toolbar.
	 * This affects the current toolbar and is also saved for future
	 * runs of Electric.
	 * @param buttons the new order of buttons in the toolbar.
	 */
	public static void setToolbarButtons(EToolBarButton[] buttons)
	{
		currentToolbarButtons = buttons;

		// build a string describing the current buttons
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<buttons.length; i++)
		{
			if (i > 0) sb.append('|');
			if (buttons[i] == null) continue;
			if (buttons[i].fullPathToIcon != null)
			{
				// button with user-specified icon
				String commandName = buttons[i].menuName+":"+buttons[i].getText();
				sb.append("U=");
				sb.append(commandName);
				sb.append("=U=");
				sb.append(buttons[i].fullPathToIcon);
			} else
			{
				// built-in button
				if (buttons[i].iconName.equals("ButtonUnknown"))
				{
					sb.append("U=");
					sb.append(buttons[i].menuName+":"+buttons[i].getText());
				} else
				{
					sb.append("B=");
					sb.append(buttons[i].iconName);
				}
			}
		}
		toolbarOrderPref.setString(sb.toString());

		if (TopLevel.isMDIMode())
		{
			ToolBar tb = TopLevel.getCurrentJFrame().getToolBar();
			tb.redoToolbar();
		} else
		{
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				ToolBar tb = wf.getFrame().getToolBar();
				tb.redoToolbar();
			}
		}
	}

	/**
	 * Method to convert an image file name into a proper-sized icon for the Toolbar.
	 * @param fileName the path to the image file.
	 * @return the toolbar icon (no more than 16 tall).
	 */
	public static ImageIcon getProperSizeIcon(String fileName)
	{
		String errorMessage = null;
		try
		{
			File f = new File(fileName);
			if (!f.exists()) errorMessage = "Missing file"; else
			{
				BufferedImage img = ImageIO.read(f);
				ImageIcon icon = new ImageIcon(img);

				// force it to be 16x16 or less
				int iconWidth = icon.getIconWidth();
				int iconHeight = icon.getIconHeight();
				int width = iconWidth;
				int height = iconHeight;
				if (height > 16)
				{
					double ratio = 16.0 / height;
					width = (int)(width * ratio);
					height = (int)(height * ratio);
					int iconWid = Math.max(width, 16);
					BufferedImage originalImage = new BufferedImage(iconWid, 16, BufferedImage.TYPE_INT_ARGB);
					Graphics g = originalImage.getGraphics();
					int dx = (iconWid-width)/2;
					int dy = (16-height)/2;
					g.drawImage(icon.getImage(), dx, dy, dx+width, dy+height, 0, 0, iconWidth, iconHeight, null);
					icon = new ImageIcon(originalImage);
				}
				return icon;
			}
		} catch (Exception e)
		{
			errorMessage = e.getMessage();
		} catch (OutOfMemoryError e)
		{
			errorMessage = "Out of Memory";
		}
		System.out.println("Unable to read icon file: " + fileName + " (" + errorMessage + ")");
		return null;
	}

	/**
	 * Method to return a mapping from command names to disk files with their icons.
	 * @return a mapping from command names to disk files with their icons.
	 */
	public static Map<String,String> getCommandToIconMap()
	{
		if (commandToIconMap == null)
		{
			commandToIconMap = new HashMap<String,String>();
			String fileMap = toolbarFilesPref.getString();
			String [] entries = fileMap.split("\t");
			for(int i=0; i<entries.length; i++)
			{
				int barPos = entries[i].indexOf('|');
				if (barPos < 0) continue;
				String commandName = entries[i].substring(0, barPos);
				String fileName = entries[i].substring(barPos+1);
				commandToIconMap.put(commandName, fileName);
			}
		}
		return commandToIconMap;
	}

	/**
	 * Method to set a mapping from command names to disk files with their icons.
	 * @param newMap a new mapping from command names to disk files with their icons.
	 */
	public static void setCommandToIconMap(Map<String,String> newMap)
	{
		commandToIconMap = newMap;
		StringBuffer sb = new StringBuffer();
		for(String commandName : commandToIconMap.keySet())
		{
			String fileName = commandToIconMap.get(commandName);
			if (sb.length() > 0) sb.append('\t');
			sb.append(commandName);
			sb.append('|');
			sb.append(fileName);
		}
		toolbarFilesPref.setString(sb.toString());
	}

	/**
	 * Method to get the default button order for the toolbar.
	 * This considers saved orders from previous runs of Electric.
	 * @return the default button order for the toolbar.
	 */
	private static EToolBarButton[] getDefaultToolbarButtons()
	{
		String prefOrder = toolbarOrderPref.getString();
		boolean buttonError = false;
		if (prefOrder.length() > 0)
		{
			// preferences set
			List<EToolBarButton> knownButtons = getAllButtons();
			EMenuBar.Instance bar = MenuCommands.menuBar().genInstance();

			String [] entries = prefOrder.split("\\|");
			EToolBarButton[] buttons = new EToolBarButton[entries.length];
			for(int i=0; i<entries.length; i++)
			{
				String entry = entries[i];
				if (entry.length() == 0) continue;
				if (entry.startsWith("B="))
				{
					// built-in button
					String buttonName = entry.substring(2);
					for(EToolBarButton known : knownButtons)
					{
						if (known.iconName.equals(buttonName))
						{
							buttons[i] = known;
							break;
						}
					}
					if (buttons[i] == null)
					{
						System.out.println("WARNING: saved tool bar button '" + buttonName + "' is unknown");
						buttonError = true;
					}
				} else if (entry.startsWith("U="))
				{
					// user-defined button
					String fullCommandName = entry.substring(2);
					String iconFile = null;
					int endPath = entry.indexOf("=U=");
					if (endPath >= 0)
					{
						fullCommandName = entry.substring(2, endPath);
						iconFile = entry.substring(endPath+3);
					}
					ImageIcon icon = unknownIcon;
					if (iconFile != null) icon = getProperSizeIcon(iconFile);
					if (icon != null)
					{
						int lastColon = fullCommandName.lastIndexOf(':');
						String menuName = (lastColon < 0) ? "" : fullCommandName.substring(0, lastColon);
						String commandName = fullCommandName.substring(lastColon+1);

						// scan all commands to find this one
						EMenuItem mi = null;
						for (EMenuItem menu: bar.getMenuBarGroup().getItems())
						{
							mi = findMenuItem((EMenu)menu, menu.getText(), fullCommandName);
							if (mi != null) break;
						}
						if (mi != null)
						{
							buttons[i] = makeButtonFromMenuItem(mi, commandName, menuName);
							if (iconFile != null) buttons[i].setIcon(icon, iconFile);
							boolean alreadyKnown = false;
							for(EToolBarButton known : allButtons)
							{
								if (known.iconName != null &&
									known.iconName.equals(fullCommandName)) { alreadyKnown = true;   break; }
							}
							if (!alreadyKnown)
								allButtons.add(buttons[i]);
						}
					}
				}
			}
			if (!buttonError) return buttons;
			toolbarOrderPref.setString("");
			System.out.println("WARNING: saved toolbar configuration has errors...using factory settings");
		}

		// use built-in default
		return getFactoryButtons();
	}

	private static EToolBarButton makeButtonFromMenuItem(EMenuItem mi, String command, String menu)
	{
		final EMenuItem item = mi;
		EToolBarButton but = new EToolBarButton(command, null, null, menu)
		{
			public void run() { item.run(); }
		};
		return but;
	}

	private void redoToolbar()
	{
		removeAll();
		EToolBarButton[] buttons = getToolbarButtons();
		boolean placedGridDistance = false;
		for (int i=0; i<buttons.length; i++)
		{
			EToolBarButton b = buttons[i];
			if (b == null) addSeparator(); else
			{
				// special case for buttons that are different in each toolbar
				if (b == goBackButtonStatic) b = goBackButton;
				if (b == goForwardButtonStatic) b = goForwardButton;
				AbstractButton j = b.genToolBarButton();
				add(j);
				j.setFocusable(false);
				if (!placedGridDistance && (b == gridSmaller || b == gridLarger))
				{
					placedGridDistance = true;
					rewriteGridDistance();
					if (!currentGridAmountInited)
					{
						currentGridAmountInited = true;
						currentGridAmount.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) { chooseGridAmount(); }
						});
					}
					add(currentGridAmount);
				}
			}
		}
		updateUI();
	}

	private void chooseGridAmount()
	{
		JPopupMenu gridMenu = new JPopupMenu("Grid Spacing");
		JMenuItem menuItem = new JMenuItem("Grid Alignment 1 (largest)");
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeGridSize(0); } });
		gridMenu.add(menuItem);
		menuItem = new JMenuItem("Grid Alignment 2");
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeGridSize(1); } });
		gridMenu.add(menuItem);
		menuItem = new JMenuItem("Grid Alignment 3");
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeGridSize(2); } });
		gridMenu.add(menuItem);
		menuItem = new JMenuItem("Grid Alignment 4");
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeGridSize(3); } });
		gridMenu.add(menuItem);
		menuItem = new JMenuItem("Grid Alignment 5 (smallest)");
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeGridSize(4); } });
		gridMenu.add(menuItem);
		gridMenu.addSeparator();
		menuItem = new JMenuItem("Grid Preferences...");
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
		{ PreferencesFrame.preferencesCommand("Grid", "Display"); } });
		gridMenu.add(menuItem);
		Point pt = currentGridAmount.getLocation();
		gridMenu.show(this, pt.x, pt.y);
	}

	private static void rewriteGridDistance()
	{
		Dimension2D val = User.getAlignmentToGrid();
		String valStr = " " + val.getWidth();
		if (val.getWidth() != val.getHeight())
			valStr += "/" + val.getHeight();
		valStr += " ";
		if (TopLevel.isMDIMode())
		{
			TopLevel tl = TopLevel.getCurrentJFrame();
			if (tl != null)
			{
				ToolBar tb = tl.getToolBar();
                if (tb != null)
                    tb.currentGridAmount.setText(valStr);
			}
		} else
		{
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				ToolBar tb = wf.getFrame().getToolBar();
				tb.currentGridAmount.setText(valStr);
			}
		}
	}

	/**
	 * Method to find the EMenuItem associated with a full command name.
	 */
	private static EMenuItem findMenuItem(EMenu menu, String menuName, String command)
	{
		for (EMenuItem menuItem : menu.getItems())
		{
			String menuItemName = menuName + ":" + menuItem.getText();
			if (menuItem instanceof EMenu)
			{
				EMenuItem found = findMenuItem((EMenu)menuItem, menuItemName, command);
				if (found != null) return found;
			} else
			{
				if (menuItemName.equals(command)) return menuItem;
			}
		}
		return null;
	}

	// --------------------------- class EToolBarBuitton ---------------------------------------------------------

	/**
	 * Generic tool bar button.
	 */
	public abstract static class EToolBarButton extends EMenuItem
	{
		/**
		 * Default icon for tool bar button instance.
		 */
		private ImageIcon defaultIcon;
		private String iconName;
		private String menuName;
		private String fullPathToIcon;

		/**
		 * @param text the menu item's displayed text.  An "_" in the string
		 * indicates the location of the "mnemonic" key for that entry.
		 * @param accelerator the shortcut key, or null if none specified.
		 * @param iconName filename without extension of default icon.
		 */
		EToolBarButton(String text, KeyStroke accelerator, String iconName, String menuName)
		{
			super(text, accelerator);
			this.iconName = iconName;
			this.menuName = menuName;
			if (iconName == null) this.defaultIcon = unknownIcon; else
				this.defaultIcon = Resources.getResource(ToolBar.class, iconName + ".gif");
		}

		/**
		 * @param text the menu item's displayed text.  An "_" in the string
		 * indicates the location of the "mnemonic" key for that entry.
		 * @param acceleratorChar the shortcut char.
		 * @param iconName filename without extension of default icon.
		 */
		EToolBarButton(String text, char acceleratorChar, String iconName, String menuName)
		{
			super(text, acceleratorChar);
			this.iconName = iconName;
			this.menuName = menuName;
			if (iconName == null) this.defaultIcon = unknownIcon; else
				this.defaultIcon = Resources.getResource(ToolBar.class, iconName + ".gif");
		}

		/**
		 * Method to return the name of the menu entry associated with this button.
		 * @return the name of the menu entry associated with this button.
		 */
		public String getMenuName() { return menuName; }

		/**
		 * Method to return the icon associated with this button.
		 * @return the icon associated with this button.
		 */
		public ImageIcon getIcon() { return defaultIcon; }

		/**
		 * Method to set the icon associated with this button.
		 * @param i the new icon associated with this button.
		 * @param path the full path to the icon file.
		 */
		public void setIcon(ImageIcon i, String path)
		{
			defaultIcon = i;
			fullPathToIcon = path;
		}

		/**
		 * Generates tool bar button item by this this generic EToolBarButton
		 * @return generated instance.
		 */
		public AbstractButton genToolBarButton()
		{
			AbstractButton b = createToolBarButton();
			b.setToolTipText(getToolTipText());
			b.setIcon(defaultIcon);
			b.addActionListener(this);
			updateToolBarButton(b);
			return b;
		}

		AbstractButton createToolBarButton() { return new JButton(); }

		/**
		 * Updates appearance of tool bar button instance after change of state.
		 */
		void updateToolBarButton(AbstractButton item)
		{
			item.setEnabled(isEnabled());
			item.setSelected(isSelected());
			item.setToolTipText(getToolTipText());
		}

		@Override
		protected void registerItem()
		{
			super.registerItem();
			registerUpdatable();
		}

		@Override
		protected void updateButtons()
		{
			updateToolBarButtons();
		}
	}

	/**
	 * Generic tool bar radio button.
	 */
	public static class EToolBarGeneralMenuButton extends EToolBarButton
	{
		private EMenuItem item;

		public EToolBarGeneralMenuButton(String text, String iconName, String menuName, EMenuItem item)
		{
			super(text, null, iconName, menuName);
			this.item = item;
		}

		public void run() { item.run(); }
	}

	/**
	 * Generic tool bar radio button.
	 */
	private abstract static class EToolBarRadioButton extends EToolBarButton
	{
		EToolBarRadioButton(String text, KeyStroke accelerator, String iconName, String menuName)
		{ super(text, accelerator, iconName, menuName); }

		EToolBarRadioButton(String text, char acceleratorChar, String iconName, String menuName)
		{ super(text, acceleratorChar, iconName, menuName); }

		@Override protected JMenuItem createMenuItem()
		{
			if (Client.isOSMac())
				return new JMenuItem();
			return new JRadioButtonMenuItem();
		}

		@Override JToggleButton createToolBarButton() { return new JToggleButton(); }
	}

	// --------------------------- Load/Save Library ---------------------------------------------------------

	public static final EToolBarButton openLibraryCommand = new EToolBarButton("_Open Library...", 'O', "ButtonOpenLibrary", "File")
	{
		@Override public void run() { FileMenu.openLibraryCommand(); }
	};

	public static final EToolBarButton saveLibraryCommand = new EToolBarButton("Sa_ve Library", null, "ButtonSaveLibrary", "File")
	{
		@Override public boolean isEnabled() { return Library.getCurrent() != null; }

		@Override public void run() { FileMenu.saveLibraryCommand(Library.getCurrent()); }
	};

	public static void setSaveLibraryButton()
	{
		updateToolBarButtons();
	}

	// --------------------------- CursorMode staff ---------------------------------------------------------

	private static CursorMode curMode = CursorMode.CLICKZOOMWIRE;

	/**
	 * CursorMode is a typesafe enum class that describes the current editing mode (select, zoom, etc).
	 */
	public static enum CursorMode {
		/** Describes ClickZoomWire mode (does everything). */  CLICKZOOMWIRE,
//		/** Describes Selection mode (click and drag). */		SELECT("Toggle Select"),
//		/** Describes wiring mode (creating arcs). */			WIRE("Toggle Wiring"),
		/** Describes Panning mode (move window contents). */	PAN,
		/** Describes Zoom mode (scale window contents). */		ZOOM,
		/** Describes Outline edit mode. */						OUTLINE,
		/** Describes Measure mode. */							MEASURE;
	}

	static final Cursor zoomCursor = readCursor("CursorZoom.gif", 6, 6);
	static final Cursor zoomOutCursor = readCursor("CursorZoomOut.gif", 6, 6);
	static final Cursor panCursor = readCursor("CursorPan.gif", 8, 8);
	static final Cursor wiringCursor = readCursor("CursorWiring.gif", 0, 0);
	static final Cursor outlineCursor = readCursor("CursorOutline.gif", 0, 8);
	static final Cursor measureCursor = readCursor("CursorMeasure.gif", 0, 0);

	public static Cursor readCursor(String cursorName, int hotX, int hotY)
	{
		ImageIcon imageIcon = Resources.getResource(ToolBar.class, cursorName);
		Image image = imageIcon.getImage();
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
		int bestWidth = (int)bestSize.getWidth();
		int bestHeight = (int)bestSize.getHeight();
		if (bestWidth != 0 && bestHeight != 0)
		{
			if (bestWidth != width || bestHeight != height)
			{
				if (bestWidth > width && bestHeight > height)
				{
					// want a larger cursor, so just pad this one
					Image newImage = new BufferedImage(bestWidth, bestHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics g = newImage.getGraphics();
					g.drawImage(image, (bestWidth-width)/2, (bestHeight-height)/2, null);
					image = newImage;
					hotX += (bestWidth-width)/2;
					hotY += (bestHeight-height)/2;
				} else
				{
					// want a smaller cursor, so scale this one
					image = image.getScaledInstance(bestWidth, bestHeight, 0);
					hotX = hotX * bestWidth / width;
					hotY = hotY * bestHeight / height;
				}
			}
		}
		Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(hotX, hotY), cursorName);
		return cursor;
	}

	/**
	 * Method to tell which cursor mode is in effect.
	 * @return the current mode (select, pan, zoom, outline, measure).
	 */
	public static CursorMode getCursorMode() { return curMode; }

	private static EventListener lastListener = null;

	/**
	 * Method to set the cursor mode (the mode of cursor interaction).
	 * The default mode is "CLICKZOOMWIRE" which does general editing.
	 * Other choices are PAN, ZOOM, OUTLINE, and MEASURE.
	 * @param cm the cursor mode to set.
	 */
	public static void setCursorMode(CursorMode cm)
	{
		changeCursorMode(cm);
		updateToolBarButtons();
	}

	private static void changeCursorMode(CursorMode cm)
	{
		switch (cm)
		{
			case CLICKZOOMWIRE:
				checkLeavingOutlineMode();
				WindowFrame.setListener(ClickZoomWireListener.theOne);
				TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				curMode = CursorMode.CLICKZOOMWIRE;
				lastListener = null;
				break;
			case PAN:
				if (WindowFrame.getListener() == ZoomAndPanListener.theOne && curMode == CursorMode.PAN)
				{
					// if there was a special mode, revert to it
					if (lastListener != null && lastListener != ClickZoomWireListener.theOne &&
						lastListener != OutlineListener.theOne && lastListener != MeasureListener.theOne)
					{
						WindowFrame.setListener(lastListener);
						curMode = CursorMode.CLICKZOOMWIRE;
						lastListener = null;
						TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						return;
					}

					// switch back to click zoom wire listener
					changeCursorMode(CursorMode.CLICKZOOMWIRE);
					return;
				}
				lastListener = WindowFrame.getListener();
				WindowFrame.setListener(ZoomAndPanListener.theOne);
				//makeCursors();
				TopLevel.setCurrentCursor(panCursor);
				curMode = CursorMode.PAN;
				break;
			case ZOOM:
				if (WindowFrame.getListener() == ZoomAndPanListener.theOne && curMode == CursorMode.ZOOM)
				{
					// if there was a special mode, revert to it
					if (lastListener != null && lastListener != ClickZoomWireListener.theOne &&
						lastListener != OutlineListener.theOne && lastListener != MeasureListener.theOne)
					{
						WindowFrame.setListener(lastListener);
						curMode = CursorMode.CLICKZOOMWIRE;
						lastListener = null;
						TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						return;
					}

					// switch back to click zoom wire listener
					changeCursorMode(CursorMode.CLICKZOOMWIRE);
					return;
				}
				lastListener = WindowFrame.getListener();
				checkLeavingOutlineMode();
				WindowFrame.setListener(ZoomAndPanListener.theOne);
				TopLevel.setCurrentCursor(zoomCursor);
				curMode = CursorMode.ZOOM;
				break;
			case OUTLINE:
				lastListener = null;
				if (WindowFrame.getListener() == OutlineListener.theOne)
				{
					// switch back to click zoom wire listener
					changeCursorMode(CursorMode.CLICKZOOMWIRE);
					return;
				}
				EditWindow wnd = EditWindow.needCurrent();
				if (wnd == null) return;
				Highlighter highlighter = wnd.getHighlighter();

				CursorMode oldMode = curMode;
				NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
				if (ni == null)
				{
					if (oldMode == CursorMode.OUTLINE) changeCursorMode(CursorMode.CLICKZOOMWIRE); else
						changeCursorMode(oldMode);
					return;
				}
				NodeProto np = ni.getProto();
				if (ni.isCellInstance() || !((PrimitiveNode)np).isHoldsOutline())
				{
					System.out.println("Sorry, " + np + " does not hold outline information");
					if (oldMode == CursorMode.OUTLINE) changeCursorMode(CursorMode.CLICKZOOMWIRE); else
						changeCursorMode(oldMode);
					return;
				}

				if (WindowFrame.getListener() != OutlineListener.theOne)
					OutlineListener.theOne.setNode(ni);
				WindowFrame.setListener(OutlineListener.theOne);
				TopLevel.setCurrentCursor(outlineCursor);
				curMode = CursorMode.OUTLINE;
				break;
			case MEASURE:
				lastListener = null;
				if (WindowFrame.getListener() == MeasureListener.theOne)
				{
					// switch back to click zoom wire listener
					changeCursorMode(CursorMode.CLICKZOOMWIRE);
					return;
				}
				checkLeavingOutlineMode();
//				MeasureListener.theOne.reset();
				WindowFrame.setListener(MeasureListener.theOne);
				TopLevel.setCurrentCursor(measureCursor);
				curMode = CursorMode.MEASURE;
				break;
		}
	}

	private static void checkLeavingOutlineMode()
	{
		// if exiting outline-edit mode, turn off special display
		if (WindowFrame.getListener() == OutlineListener.theOne && curMode == CursorMode.OUTLINE)
		{
			EditWindow wnd = EditWindow.needCurrent();
			if (wnd != null)
			{
				Highlighter highlighter = wnd.getHighlighter();
				NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
				if (ni != null)
				{
					Highlight high = highlighter.getOneHighlight();
					if (high != null)
					{
						high.setPoint(-1);
						wnd.repaint();
					}
				}
			}
		}
	}

	private static final CursorModeButton clickZoomWireCommand = new CursorModeButton("Click/Zoom/Wire", 'S', "ButtonClickZoomWire",
		"Edit:Modes:Edit", CursorMode.CLICKZOOMWIRE);
	private static final CursorModeButton panCommand = new CursorModeButton("Toggle Pan", 'P', "ButtonPan",
		"Edit:Modes:Edit", CursorMode.PAN);
	private static final CursorModeButton zoomCommand = new CursorModeButton("Toggle Zoom", 'Z', "ButtonZoom",
		"Edit:Modes:Edit", CursorMode.ZOOM);
	private static final CursorModeButton outlineCommand = new CursorModeButton("Toggle Outline Edit", 'Y', "ButtonOutline",
		"Edit:Modes:Edit", CursorMode.OUTLINE);
	private static final CursorModeButton measureCommand = new CursorModeButton("Toggle Measure Distance", 'M', "ButtonMeasure",
		"Edit:Modes:Edit", CursorMode.MEASURE);

	private static class CursorModeButton extends EToolBarRadioButton
	{
		private final CursorMode cm;
		CursorModeButton(String text, char acceleratorChar, String iconName, String menuName, CursorMode cm)
		{
			super(text, KeyStroke.getKeyStroke(acceleratorChar, 0), iconName, menuName);
			this.cm = cm;
		}
		@Override public boolean isSelected() { return getCursorMode() == cm; }
		@Override public void run() { changeCursorMode(cm); }
	}

	// --------------------------- ArrowDistance staff ---------------------------------------------------------

	/**
	 * Method to signal ToolBar that gridAlignment changed
	 */
	public static void setGridAligment() { updateToolBarButtons(); }

	private static EMenuItem gridDistance1Command = new EMenuItem("Grid Alignment 1 (largest)") { public void run() {
		changeGridSize(0); }};
	private static EMenuItem gridDistance2Command = new EMenuItem("Grid Alignment 2") { public void run() {
		changeGridSize(1); }};
	private static EMenuItem gridDistance3Command = new EMenuItem("Grid Alignment 3") { public void run() {
		changeGridSize(2); }};
	private static EMenuItem gridDistance4Command = new EMenuItem("Grid Alignment 4") { public void run() {
		changeGridSize(3); }};
	private static EMenuItem gridDistance5Command = new EMenuItem("Grid Alignment 5 (smallest)") { public void run() {
		changeGridSize(4); }};
	private JButton currentGridAmount = new JButton();
	private boolean currentGridAmountInited = false;

	private static final EToolBarButton gridLarger = new EToolBarButton("Make Grid Larger",
		KeyStroke.getKeyStroke('F', 0), "ButtonGridCoarser", "Edit:Modes:Movement")
    {
        public void run() { changeGridSize(true); }
        public boolean isEnabled()
        {
            int index = User.getAlignmentToGridIndex();
            return (index != 0); // most right
        }
    };

	private static final EToolBarButton gridSmaller = new EToolBarButton("Make Grid Smaller",
		KeyStroke.getKeyStroke('H', 0), "ButtonGridFiner", "Edit:Modes:Movement")
    {
        public void run() { changeGridSize(false); }
        public boolean isEnabled()
        {
        	Dimension2D[] vals = User.getAlignmentToGridVector();
            int index = User.getAlignmentToGridIndex();
            return (index != vals.length - 1); // most right
        }
    };

	private static void changeGridSize(int size)
	{
		Dimension2D[] vals = User.getAlignmentToGridVector();
		User.setAlignmentToGridVector(vals, size);
		rewriteGridDistance();
		updateToolBarButtons();
	}

    private static void changeGridSize(boolean larger)
	{
    	Dimension2D[] vals = User.getAlignmentToGridVector();
        int i = User.getAlignmentToGridIndex();

        if (larger)
        {
            if (i > 0)
            {
                User.setAlignmentToGridVector(vals, i-1);
                rewriteGridDistance();
            }
        } else
        {
            if (i < vals.length-1)
            {
                User.setAlignmentToGridVector(vals, i+1);
                rewriteGridDistance();
            }
        }
	}

	// --------------------------- SelectMode staff ---------------------------------------------------------

	private static SelectMode curSelectMode = SelectMode.OBJECTS;

	/**
	 * SelectMode is a typesafe enum class that describes the current selection modes (objects or area).
	 */
	public static enum SelectMode
	{
		/** Describes Selection mode (click and drag). */		OBJECTS,
		/** Describes Selection mode (click and drag). */		AREA;
	}

	/**
	 * Method to tell what selection mode is in effect.
	 * @return the current selection mode (objects or area).
	 */
	public static SelectMode getSelectMode() { return curSelectMode; }

	private static void setSelectMode(SelectMode selectMode) { curSelectMode = selectMode; }

	private static final SelectModeButton selectObjectsCommand = new SelectModeButton("Select Objects", "ButtonObjects",
		"Edit:Modes:Select", SelectMode.OBJECTS);
	private static final SelectModeButton selectAreaCommand = new SelectModeButton("Select Area", "ButtonArea",
		"Edit:Modes:Select", SelectMode.AREA);

	public static class SelectModeButton extends EToolBarRadioButton
	{
		private final SelectMode sm;

		SelectModeButton(String text, String iconName, String menuName, SelectMode sm)
		{
			super(text, null, iconName, menuName);
			this.sm = sm;
		}

		@Override public boolean isSelected() { return getSelectMode() == sm; }
		@Override public void run() { setSelectMode(sm); }
	}

	// --------------------------- SelectSpecial staff ---------------------------------------------------------

	private static boolean selectSpecial = false;

	/**
	 * Returns state of "select special" button
	 * @return true if select special button selected, false otherwise
	 */
	public static boolean isSelectSpecial() { return selectSpecial; }

	/**
	 * Method called to toggle the state of the "select special"
	 * button.
	 */
	private static void setSelectSpecial(boolean b) { selectSpecial = b; }

	private static final ImageIcon selectSpecialIconOn = Resources.getResource(ToolBar.class, "ButtonSelectSpecialOn.gif");
	private static final ImageIcon selectSpecialIconOff = Resources.getResource(ToolBar.class, "ButtonSelectSpecialOff.gif");

	private static EToolBarButton toggleSelectSpecialCommand = new EToolBarButton("Toggle Special Select", null, "ButtonSelectSpecialOff",
		"Edit:Modes:Select")
	{
		public boolean isSelected() { return isSelectSpecial(); }
		@Override protected JMenuItem createMenuItem() { return new JCheckBoxMenuItem(); }
		@Override AbstractButton createToolBarButton() { return new JToggleButton(); }
		@Override public void run() { setSelectSpecial(!isSelectSpecial()); }
		@Override void updateToolBarButton(AbstractButton item)
		{
			super.updateToolBarButton(item);
			item.setSelected(isSelected());
			item.setIcon(isSelected() ? selectSpecialIconOn : selectSpecialIconOff);
		}
	};

	// --------------------------- Modes submenu of Edit menu ---------------------------------------------------------

	// mnemonic keys available: ABCD FGHIJKL NOPQR TUVWXYZ
	public static final EMenu modesSubMenu = new EMenu("M_odes",

		new EMenu("_Edit",
			clickZoomWireCommand,
			panCommand,
			zoomCommand,
			outlineCommand,
			measureCommand),

		new EMenu("_Movement",
			gridLarger,
			gridDistance1Command,
			gridDistance2Command,
			gridDistance3Command,
			gridDistance4Command,
			gridDistance5Command,
			gridSmaller),

		new EMenu("_Select",
			selectObjectsCommand,
			selectAreaCommand,
			toggleSelectSpecialCommand)
	);

	// --------------------------- Misc commands ---------------------------------------------------------

	public static final EToolBarButton preferencesCommand = new EToolBarButton("P_references...", null, "ButtonPreferences", "File")
	{
		public void run() { PreferencesFrame.preferencesCommand(); }
	};

	public static final EToolBarButton expandOneLevelCommand = new EToolBarButton("_One Level Down", null, "ButtonExpand",
		"Cell:Expand Cell Instances")
	{
		public void run() { CircuitChanges.DoExpandCommands(false, 1); }
	};

	public static final EToolBarButton unexpandOneLevelCommand = new EToolBarButton("_One Level Up", null, "ButtonUnexpand",
		"Cell:Unexpand Cell Instances")
	{
		public void run() { CircuitChanges.DoExpandCommands(true, 1); }
	};

	// --------------------------- Undo/Redo staff ---------------------------------------------------------

	public static final EToolBarButton undoCommand = new EToolBarButton("_Undo", 'Z', "ButtonUndo", "Edit")
	{
		public void run()
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null && wf.getContent() instanceof TextWindow)
			{
				// handle undo in text windows specially
				TextWindow tw = (TextWindow)wf.getContent();
				tw.undo();
			} else
			{
				// if there is edit-in-place going on, undo that
				EditWindow wnd = EditWindow.getCurrent();
				if (wnd != null)
				{
					GetInfoText.EditInPlaceListener eip = wnd.getInPlaceTextObject();
					if (eip != null)
					{
						eip.undo();
						return;
					}
				}

				// do database undo
                String task = Undo.getUndoActivity();
                boolean realUndo = true; // always undo by default
                if (task.length() == 0) realUndo = false;
                boolean readP = task.equals(FileMenu.openJobName);
                if ((readP || task.startsWith("Write")) && realUndo)
                {
                    String [] options = {"Cancel", "Undo"};
                    String msg = (readP) ? "Reading" : "Writing";
                    String extra = (!readP) ? ". Undo won't modify the file on disk." : "";
                    int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Undo the " + msg + " process?" + extra, "Undo " + msg,
						JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Cancel");
					if (ret == 0)
                        realUndo = false;
                }
//                if (realUndo && task.startsWith("Write"))
//                {
//                    JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Write can not be undone");
//                    realUndo = false;
//                    Undo.removeLastChangeBatchTask();
//                }
                if (realUndo)
                    Undo.undo();
			}
		}
		public boolean isEnabled() { return UserInterfaceMain.getUndoEnabled(); }
	};

	public static final EToolBarButton redoCommand = new EToolBarButton("Re_do", 'Y', "ButtonRedo", "Edit")
	{
		public void run()
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null && wf.getContent() instanceof TextWindow)
			{
				// handle redo in text windows specially
				TextWindow tw = (TextWindow)wf.getContent();
				tw.redo();
			} else
			{
				// if there is edit-in-place going on, redo that
				EditWindow wnd = EditWindow.getCurrent();
				if (wnd != null)
				{
					GetInfoText.EditInPlaceListener eip = wnd.getInPlaceTextObject();
					if (eip != null)
					{
						eip.redo();
						return;
					}
				}

				// do database redo
				Undo.redo();
			}
		}
		public boolean isEnabled() { return UserInterfaceMain.getRedoEnabled(); }
	};

	public static void updateUndoRedoButtons(boolean undo, boolean redo)
	{
		updateToolBarButtons();
	}

	// --------------------------- CellHistory stuff ---------------------------------------------------------

	private static final EToolBarButton goBackButtonStatic = new EToolBarButton("Go Back a Cell", null, "ButtonGoBack",
		"Cell:Cell Viewing History") { public void run() {} };

	private static final EToolBarButton goForwardButtonStatic = new EToolBarButton("Go Forward a Cell", null, "ButtonGoForward",
		"Cell:Cell Viewing History") { public void run() {} };

	/** Go back button */
	private CellHistoryButton goBackButton = new CellHistoryButton("Go Back a Cell", "ButtonGoBack",
		"Cell:Cell Viewing History")
	{
		public void run()
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null) wf.cellHistoryGoBack();
		}
	};

	/** Go forward button */
	private CellHistoryButton goForwardButton = new CellHistoryButton("Go Forward a Cell", "ButtonGoForward",
		"Cell:Cell Viewing History")
	{
		public void run()
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null) wf.cellHistoryGoForward();
		}
	};

	private static abstract class CellHistoryButton extends EToolBarButton implements MouseListener
	{
		private boolean enabled;
		CellHistoryButton(String text, String iconName, String menuName) { super(text, null, iconName, menuName); }

		@Override public AbstractButton genToolBarButton()
		{
			AbstractButton b = super.genToolBarButton();
			b.addMouseListener(this);
			return b;
		}

		public boolean isEnabled() { return enabled; }
		void setEnabled(boolean b) { enabled = b; }

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e)
		{
			AbstractButton b = (AbstractButton) e.getSource();
			if(ClickZoomWireListener.isRightMouse(e) && b.contains(e.getX(), e.getY()))
				showHistoryPopup(e);
		}
	}

	/**
	 * Update CellHistory buttons on this ToolBar
	 * @param backEnabled true to enable goBackButton.
	 * @param forwardEnabled true toenable goForwardButton.
	 */
	public void updateCellHistoryStatus(boolean backEnabled, boolean forwardEnabled)
	{
		goBackButton.setEnabled(backEnabled);
		goForwardButton.setEnabled(forwardEnabled);
		updateToolBarButtons();
	}

	private static void showHistoryPopup(MouseEvent e)
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		List<WindowFrame.CellHistory> historyList = wf.getCellHistoryList();
		int location = wf.getCellHistoryLocation();

		JPopupMenu popup = new JPopupMenu();
		Map<Cell,Cell> listed = new HashMap<Cell,Cell>();
		for (int i=historyList.size()-1; i > -1; i--)
		{
			WindowFrame.CellHistory entry = historyList.get(i);
			Cell cell = entry.getCell();
			// skip if already shown such a cell
			if (cell == null) continue;
			if (listed.get(cell) != null) continue;
			listed.put(cell, cell);

			boolean shown = (i == location);
			JMenuItem m = new JMenuItem(cell.noLibDescribe() + (shown? "  (shown)" : ""));
			m.addActionListener(new HistoryPopupAction(wf, i));
			popup.add(m);
		}
		Component invoker = e.getComponent();
		if (invoker != null)
		{
			popup.setInvoker(invoker);
			Point2D loc = invoker.getLocationOnScreen();
			popup.setLocation((int)loc.getX() + invoker.getWidth()/2, (int)loc.getY() + invoker.getHeight()/2);
		}
		popup.setVisible(true);
	}

	private static class HistoryPopupAction implements ActionListener
	{
		private final WindowFrame wf;
		private final int historyLocation;

		private HistoryPopupAction(WindowFrame wf, int loc)
		{
			this.wf = wf;
			this.historyLocation = loc;
		}

		public void actionPerformed(ActionEvent e) { wf.setCellByHistory(historyLocation); }
	}

	// ----------------------------------------------------------------------------

	/**
	 * Update associated ToolBarButtons on all toolbars and updatable menu items on all menubars
	 */
	public static void updateToolBarButtons()
	{
		for (ToolBar toolBar: TopLevel.getToolBars())
		{
            if (toolBar == null) continue;
			for (Component c: toolBar.getComponents())
			{
				if (c == toolBar.currentGridAmount)
				{
					rewriteGridDistance();
					continue;
				}
				if (!(c instanceof AbstractButton)) continue;
				AbstractButton b = (AbstractButton)c;
				for (ActionListener a: b.getActionListeners())
				{
					if (a instanceof EToolBarButton)
					{
						EToolBarButton tbb = (EToolBarButton)a;
						// special case for buttons that are different in each toolbar
						if (tbb == goBackButtonStatic) tbb = toolBar.goBackButton;
						if (tbb == goForwardButtonStatic) tbb = toolBar.goForwardButton;
						tbb.updateToolBarButton(b);
					}
				}
			}
		}
		MenuCommands.menuBar().updateAllButtons();
	}

	/**
	 * Call when done with this toolBar to release its resources
	 */
	public void finished() {}
}
