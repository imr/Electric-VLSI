/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetInfoMulti.java
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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;


/**
 * Class to handle the "Multi-object Get Info" dialog.
 */
public class GetInfoMulti extends EDialog implements HighlightListener, DatabaseChangeListener
{
	private static final int CHANGEXSIZE           = 1;
	private static final int CHANGEYSIZE           = 2;
	private static final int CHANGEXPOS            = 3;
	private static final int CHANGEYPOS            = 4;
	private static final int CHANGEROTATION        = 5;
	private static final int CHANGEMIRRORLR        = 6;
	private static final int CHANGEMIRRORUD        = 7;
	private static final int CHANGEEXPANDED        = 8;
	private static final int CHANGEEASYSELECT      = 9;
	private static final int CHANGEINVOUTSIDECELL  = 10;
	private static final int CHANGELOCKED          = 11;
	private static final int CHANGEWIDTH           = 12;
	private static final int CHANGERIGID           = 13;
	private static final int CHANGEFIXANGLE        = 14;
	private static final int CHANGESLIDABLE        = 15;
	private static final int CHANGEEXTENSION       = 16;
	private static final int CHANGEDIRECTION       = 17;
	private static final int CHANGENEGATION        = 18;
	private static final int CHANGECHARACTERISTICS = 19;
	private static final int CHANGEBODYONLY        = 20;
	private static final int CHANGEALWAYSDRAWN     = 21;
	private static final int CHANGEPOINTSIZE       = 22;
	private static final int CHANGEUNITSIZE        = 23;
	private static final int CHANGEXOFF            = 24;
	private static final int CHANGEYOFF            = 25;
	private static final int CHANGETEXTROT         = 26;
	private static final int CHANGEANCHOR          = 27;
	private static final int CHANGEFONT            = 28;
	private static final int CHANGECOLOR           = 29;
	private static final int CHANGEBOLD            = 30;
	private static final int CHANGEITALIC          = 31;
	private static final int CHANGEUNDERLINE       = 32;
	private static final int CHANGECODE            = 33;
	private static final int CHANGEUNITS           = 34;
	private static final int CHANGESHOW            = 35;
	
	private static GetInfoMulti theDialog = null;
	private DefaultListModel listModel;
	private JList list;
	private JPanel changePanel;
	private int [] currentChangeTypes;
	private JComponent [] currentChangeValues;
	private List<Highlight2> highlightList;
	List<NodeInst> nodeList;
	List<ArcInst>arcList;
	List<Export> exportList;
	List<Highlight2> textList;

    private EditWindow wnd;

	private static final int [] nodeChanges = {CHANGEXSIZE, CHANGEYSIZE, CHANGEXPOS, CHANGEYPOS, CHANGEROTATION,
		CHANGEMIRRORLR, CHANGEMIRRORUD, CHANGEEXPANDED, CHANGEEASYSELECT, CHANGEINVOUTSIDECELL, CHANGELOCKED};
	private static final int [] arcChanges = {CHANGEWIDTH, CHANGERIGID, CHANGEFIXANGLE, CHANGESLIDABLE,
		CHANGEEXTENSION, CHANGEDIRECTION, CHANGENEGATION, CHANGEEASYSELECT};
	private static final int [] exportChanges = {CHANGECHARACTERISTICS, CHANGEBODYONLY, CHANGEALWAYSDRAWN,
		CHANGEPOINTSIZE, CHANGEUNITSIZE, CHANGEXOFF, CHANGEYOFF, CHANGETEXTROT, CHANGEANCHOR, CHANGEFONT,
		CHANGECOLOR, CHANGEBOLD, CHANGEITALIC, CHANGEUNDERLINE, CHANGEINVOUTSIDECELL};
	private static final int [] textChanges = {CHANGEPOINTSIZE, CHANGEUNITSIZE, CHANGEXOFF, CHANGEYOFF,
		CHANGETEXTROT, CHANGEANCHOR, CHANGEFONT, CHANGECOLOR, CHANGECODE, CHANGEUNITS, CHANGESHOW,
		CHANGEBOLD, CHANGEITALIC, CHANGEUNDERLINE, CHANGEINVOUTSIDECELL};
	private static final int [] nodeArcChanges = {CHANGEEASYSELECT};
	private static final int [] nodeTextChanges = {CHANGEINVOUTSIDECELL};
	private static final int [] nodeExportChanges = {CHANGEINVOUTSIDECELL};
	private static final int [] nodeTextExportChanges = {CHANGEINVOUTSIDECELL};
	private static final int [] textExportChanges = {CHANGEPOINTSIZE, CHANGEUNITSIZE, CHANGEXOFF, CHANGEYOFF,
		CHANGETEXTROT, CHANGEANCHOR, CHANGEFONT, CHANGECOLOR, CHANGEBOLD, CHANGEITALIC, CHANGEUNDERLINE, CHANGEINVOUTSIDECELL};

	private static final int [][] changeCombos =
	{
		null,						//
		nodeChanges,				// nodes
		arcChanges,					//       arcs
		nodeArcChanges,				// nodes arcs
		exportChanges,				//            exports
		nodeExportChanges,			// nodes      exports
		null,						//       arcs exports
		null,						// nodes arcs exports
		textChanges,				//                    text
		nodeTextChanges,			// nodes              text
		null,						//       arcs         text
		null,						// nodes arcs         text
		textExportChanges,			//            exports text
		nodeTextExportChanges,		// nodes      exports text
		null,						//       arcs exports text
		null,						// nodes arcs exports text
	};

	/**
	 * Method to show the Multi-object Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
            JFrame jf;
            if (TopLevel.isMDIMode())
			    jf = TopLevel.getCurrentJFrame();
            else
                jf = null;
			theDialog = new GetInfoMulti(jf, false);
		}
        theDialog.loadMultiInfo();
        theDialog.pack();
		theDialog.setVisible(true);
	}

	/**
	 * Reloads the dialog when Highlights change
	 */
	public void highlightChanged(Highlighter which)
	{
        if (!isVisible()) return;
		Dimension oldDim = listPane.getSize();
		loadMultiInfo();
		listPane.setPreferredSize(oldDim);
		pack();
	}

    /**
     * Called when by a Highlighter when it loses focus. The argument
     * is the Highlighter that has gained focus (may be null).
     * @param highlighterGainedFocus the highlighter for the current window (may be null).
     */
    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {
        if (!isVisible()) return;
        loadMultiInfo();        
    }

    /**
     * Respond to database changes we care about
     * @param e database change event
     */
    public void databaseChanged(DatabaseChangeEvent e) {
        if (!isVisible()) return;

        boolean reload = false;
        // reload if any objects that changed are part of our list of highlighted objects
		for (Highlight2 h : highlightList) {
			if (e.objectChanged(h.getElectricObject())) {
				reload = true; break;
			}
		}
        if (reload) {
            // update dialog
            loadMultiInfo();
			pack();
        }
    }

	/** Creates new form Multi-Object Get Info */
	private GetInfoMulti(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		highlightList = new ArrayList<Highlight2>();
		initComponents();
        getRootPane().setDefaultButton(ok);

        Undo.addDatabaseChangeListener(this);

		// make the list of selected objects
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		listPane.setViewportView(list);

		// make the panel for changes
		changePanel = new JPanel();
		changePanel.setLayout(new BoxLayout(changePanel, BoxLayout.Y_AXIS));
		possibleChanges.setViewportView(changePanel);

		loadMultiInfo();
		pack();
		finishInitialization();
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	private void loadMultiInfo()
	{
       // update current window
        EditWindow curWnd = EditWindow.getCurrent();
        if ((wnd != curWnd) && (curWnd != null)) {
            if (wnd != null) wnd.getHighlighter().removeHighlightListener(this);
            curWnd.getHighlighter().addHighlightListener(this);
            wnd = curWnd;
        }

		// copy the selected objects to a private list and sort it
		highlightList.clear();
        if (wnd != null) {
            for(Highlight2 h: wnd.getHighlighter().getHighlights())
            {
                highlightList.add(h);
            }
            Collections.sort(highlightList, new SortMultipleHighlights());
        }

		// show the list
		nodeList = new ArrayList<NodeInst>();
		arcList = new ArrayList<ArcInst>();
		exportList = new ArrayList<Export>();
		textList = new ArrayList<Highlight2>();
		Geometric firstGeom = null, secondGeom = null;
		double xPositionLow = Double.MAX_VALUE, xPositionHigh = -Double.MAX_VALUE;
		double yPositionLow = Double.MAX_VALUE, yPositionHigh = -Double.MAX_VALUE;
		double xSizeLow = Double.MAX_VALUE, xSizeHigh = -Double.MAX_VALUE;
		double ySizeLow = Double.MAX_VALUE, ySizeHigh = -Double.MAX_VALUE;
		double widthLow = Double.MAX_VALUE, widthHigh = -Double.MAX_VALUE;
		selectionCount.setText(Integer.toString(highlightList.size()) + " selections:");
		List<String> displayList = new ArrayList<String>();
        for(Highlight2 h : highlightList)
		{
			ElectricObject eobj = h.getElectricObject();
            h.getInfo(displayList);
			if (h.isHighlightEOBJ())
			{
				if (eobj instanceof PortInst)
					eobj = ((PortInst)eobj).getNodeInst();
				if (eobj instanceof Geometric)
				{
					if (firstGeom == null) firstGeom = (Geometric)eobj; else
						if (secondGeom == null) secondGeom = (Geometric)eobj;
				}
				if (eobj instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)eobj;
					nodeList.add(ni);

					xPositionLow = Math.min(xPositionLow, ni.getAnchorCenterX());
					xPositionHigh = Math.max(xPositionHigh, ni.getAnchorCenterX());
					yPositionLow = Math.min(yPositionLow, ni.getAnchorCenterY());
					yPositionHigh = Math.max(yPositionHigh, ni.getAnchorCenterY());

					SizeOffset so = ni.getSizeOffset();
			        double xVal = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
					double yVal = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
			        if (ni.getAngle() == 900 || ni.getAngle() == 2700)
					{
						double swap = xVal;   xVal = yVal;   yVal = swap;
					}
					xSizeLow = Math.min(xSizeLow, xVal);
					xSizeHigh = Math.max(xSizeHigh, xVal);
					ySizeLow = Math.min(ySizeLow, yVal);
					ySizeHigh = Math.max(ySizeHigh, yVal);
				} else if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					arcList.add(ai);
					double trueWidth = ai.getWidth() - ai.getProto().getWidthOffset();
					widthLow = Math.min(widthLow, trueWidth);
					widthHigh = Math.max(widthHigh, trueWidth);
				}
			} else if (h.isHighlightText())
			{
				if (h.getVar() != null)
				{
					textList.add(h);
				} else
				{
					if (h.getName() != null)
					{
                        textList.add(h);
					} else if (eobj instanceof Export)
					{
						exportList.add((Export)eobj);
					} else if (eobj instanceof NodeInst)
					{
						textList.add(h);
					}
				}
			}
		}

		// reload the list (much more efficient than clearing and reloading if it is already displayed)
		list.setListData(displayList.toArray());

		// with exactly 2 objects, show the distance between them
		if (nodeList.size() + arcList.size() == 2)
		{
			listModel.addElement("---------------------------");
			Point2D firstPt = firstGeom.getTrueCenter();
			if (firstGeom instanceof NodeInst) firstPt = ((NodeInst)firstGeom).getAnchorCenter();
			Point2D secondPt = secondGeom.getTrueCenter();
			if (secondGeom instanceof NodeInst) secondPt = ((NodeInst)secondGeom).getAnchorCenter();
			listModel.addElement("Distance between centers: X=" + Math.abs(firstPt.getX() - secondPt.getX()) +
			   " Y=" + Math.abs(firstPt.getY() - secondPt.getY()));
		}

		// figure out what can be edited here
		int index = 0;
		if (nodeList.size() != 0) index += 1;
		if (arcList.size() != 0) index += 2;
		if (exportList.size() != 0) index += 4;
		if (textList.size() != 0) index += 8;

		changePanel.removeAll();
		currentChangeTypes = changeCombos[index];
		if (currentChangeTypes == null) return;
		currentChangeValues = new JComponent[currentChangeTypes.length];
		if (currentChangeTypes != null)
		{
			for(int c=0; c<currentChangeTypes.length; c++)
			{
				int change = currentChangeTypes[c];
				JPanel onePanel = new JPanel();
				onePanel.setLayout(new GridBagLayout());
	            String msg = null;
				switch (change)
				{
					case CHANGEXSIZE:
						if (xSizeLow == xSizeHigh) msg = "(All are " + TextUtils.formatDouble(xSizeLow) + ")"; else
							msg = "(" + TextUtils.formatDouble(xSizeLow) + " to " + TextUtils.formatDouble(xSizeHigh) + ")";
						addChangePossibility("X size:", currentChangeValues[c] = new JTextField(""), msg, onePanel);
						break;
					case CHANGEYSIZE:
						if (ySizeLow == ySizeHigh) msg = "(All are " + TextUtils.formatDouble(ySizeLow) + ")"; else
							msg = "(" + TextUtils.formatDouble(ySizeLow) + " to " + TextUtils.formatDouble(ySizeHigh) + ")";
						addChangePossibility("Y size:", currentChangeValues[c] = new JTextField(""), msg, onePanel);
						break;
					case CHANGEXPOS:
						if (xPositionLow == xPositionHigh) msg = "(All are " + TextUtils.formatDouble(xPositionLow) + ")"; else
							msg = "(" + TextUtils.formatDouble(xPositionLow) + " to " + TextUtils.formatDouble(xPositionHigh) + ")";
						addChangePossibility("X position:", currentChangeValues[c] = new JTextField(""), msg, onePanel);
						break;
					case CHANGEYPOS:
						if (yPositionLow == yPositionHigh) msg = "(All are " + TextUtils.formatDouble(yPositionLow) + ")"; else
							msg = "(" + TextUtils.formatDouble(yPositionLow) + " to " + TextUtils.formatDouble(yPositionHigh) + ")";
						addChangePossibility("Y position:", currentChangeValues[c] = new JTextField(""), msg, onePanel);
						break;
					case CHANGEROTATION:
						addChangePossibility("Rotation:", currentChangeValues[c] = new JTextField(), null, onePanel);
						break;
					case CHANGEMIRRORLR:
						JComboBox lr = new JComboBox();
						lr.addItem("Leave alone");   lr.addItem("Set");   lr.addItem("Clear");
						addChangePossibility("Mirror L-R:", currentChangeValues[c] = lr, null, onePanel);
						break;
					case CHANGEMIRRORUD:
						JComboBox ud = new JComboBox();
						ud.addItem("Leave alone");   ud.addItem("Set");   ud.addItem("Clear");
						addChangePossibility("Mirror U-D:", currentChangeValues[c] = ud, null, onePanel);
						break;
					case CHANGEEXPANDED:
						JComboBox exp = new JComboBox();
						exp.addItem("Leave alone");   exp.addItem("Expand");   exp.addItem("Unexpand");
						addChangePossibility("Expansion:", currentChangeValues[c] = exp, null, onePanel);
						break;
					case CHANGEEASYSELECT:
						JComboBox es = new JComboBox();
						es.addItem("Leave alone");   es.addItem("Make Easy");   es.addItem("Make Hard");
						addChangePossibility("Ease of Selection:", currentChangeValues[c] = es, null, onePanel);
						break;
					case CHANGEINVOUTSIDECELL:
						JComboBox io = new JComboBox();
						io.addItem("Leave alone");   io.addItem("Make Invisible");   io.addItem("Make Visible");
						addChangePossibility("Invisible Outside Cell:", currentChangeValues[c] = io, null, onePanel);
						break;
					case CHANGELOCKED:
						JComboBox lo = new JComboBox();
						lo.addItem("Leave alone");   lo.addItem("Lock");   lo.addItem("Unlock");
						addChangePossibility("Locked:", currentChangeValues[c] = lo, null, onePanel);
						break;
					case CHANGEWIDTH:
						if (widthLow == widthHigh) msg = "(All are " + TextUtils.formatDouble(widthLow) + ")"; else
							msg = "(" + TextUtils.formatDouble(widthLow) + " to " + TextUtils.formatDouble(widthHigh) + ")";
						addChangePossibility("Width:", currentChangeValues[c] = new JTextField(""), msg, onePanel);
						break;
					case CHANGERIGID:
						JComboBox ri = new JComboBox();
						ri.addItem("Leave alone");   ri.addItem("Make Rigid");   ri.addItem("Make Unrigid");
						addChangePossibility("Rigid:", currentChangeValues[c] = ri, null, onePanel);
						break;
					case CHANGEFIXANGLE:
						JComboBox fa = new JComboBox();
						fa.addItem("Leave alone");   fa.addItem("Make Fixed Angle");   fa.addItem("Make Not Fixed Angle");
						addChangePossibility("Fixed Angle:", currentChangeValues[c] = fa, null, onePanel);
						break;
					case CHANGESLIDABLE:
						JComboBox sl = new JComboBox();
						sl.addItem("Leave alone");   sl.addItem("Make Slidable");   sl.addItem("Make Not Slidable");
						addChangePossibility("Slidable:", currentChangeValues[c] = sl, null, onePanel);
						break;
					case CHANGEEXTENSION:
						JComboBox ex = new JComboBox();
						ex.addItem("Leave alone");   ex.addItem("Make Both Ends Extend");   ex.addItem("Make Neither End Extend");
						ex.addItem("Make Head Extend");   ex.addItem("Make Tail Extend");
						addChangePossibility("Extension:", currentChangeValues[c] = ex, null, onePanel);
						break;
					case CHANGEDIRECTION:
						JComboBox di = new JComboBox();
						di.addItem("Leave alone");   di.addItem("No directional arrow");
						di.addItem("Arrow on Head and Body");   di.addItem("Arrow on Tail and Body");
						di.addItem("Arrow on Body Only");   di.addItem("Arrow on Head, Tail, and Body");
						addChangePossibility("Directionality:", currentChangeValues[c] = di, null, onePanel);
						break;
					case CHANGENEGATION:
						JComboBox ne = new JComboBox();
						ne.addItem("Leave alone");   ne.addItem("No Negation");   ne.addItem("Negate Head");
						ne.addItem("Negate Tail");   ne.addItem("Negate Head and Tail");
						addChangePossibility("Negation:", currentChangeValues[c] = ne, null, onePanel);
						break;
					case CHANGECHARACTERISTICS:
						JComboBox ch = new JComboBox();
						ch.addItem("Leave alone");
						List<PortCharacteristic> chList = PortCharacteristic.getOrderedCharacteristics();
						for(PortCharacteristic chara : chList)
						{
							ch.addItem(chara.getName());
						}
						addChangePossibility("Characteristics:", currentChangeValues[c] = ch, null, onePanel);
						break;
					case CHANGEBODYONLY:
						JComboBox bo = new JComboBox();
						bo.addItem("Leave alone");   bo.addItem("Make Body Only");   bo.addItem("Make Not Body Only");
						addChangePossibility("Body Only:", currentChangeValues[c] = bo, null, onePanel);
						break;
					case CHANGEALWAYSDRAWN:
						JComboBox ad = new JComboBox();
						ad.addItem("Leave alone");   ad.addItem("Make Always Drawn");   ad.addItem("Make Not Always Drawn");
						addChangePossibility("Always Drawn:", currentChangeValues[c] = ad, null, onePanel);
						break;
					case CHANGEPOINTSIZE:
						addChangePossibility("Point Size:", currentChangeValues[c] = new JTextField(""), null, onePanel);
						break;
					case CHANGEUNITSIZE:
						addChangePossibility("Unit Size:", currentChangeValues[c] = new JTextField(""), null, onePanel);
						break;
					case CHANGEXOFF:
						addChangePossibility("X Offset:", currentChangeValues[c] = new JTextField(""), null, onePanel);
						break;
					case CHANGEYOFF:
						addChangePossibility("Y Offset:", currentChangeValues[c] = new JTextField(""), null, onePanel);
						break;
					case CHANGETEXTROT:
						JComboBox tr = new JComboBox();
						tr.addItem("Leave alone");   tr.addItem("No Rotation");   tr.addItem("Rotate 90 Degrees");
						tr.addItem("Rotate 180 Degrees");   tr.addItem("Rotate 270 Degrees");
						addChangePossibility("Text Rotation:", currentChangeValues[c] = tr, null, onePanel);
						break;
					case CHANGEANCHOR:
						JComboBox an = new JComboBox();
						an.addItem("Leave alone");
				        for(Iterator<TextDescriptor.Position> it = TextDescriptor.Position.getPositions(); it.hasNext(); )
						{
				            TextDescriptor.Position pos = it.next();
				            an.addItem(pos);
				        }
						addChangePossibility("Text Anchor:", currentChangeValues[c] = an, null, onePanel);
						break;
					case CHANGEFONT:
						JComboBox fo = new JComboBox();
						fo.addItem("Leave alone");
				        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
				        for(int i=0; i<fonts.length; i++)
				            fo.addItem(fonts[i].getFontName());
						addChangePossibility("Text Font:", currentChangeValues[c] = fo, null, onePanel);
						break;
					case CHANGECOLOR:
						JComboBox co = new JComboBox();
						co.addItem("Leave alone");
				        int [] colorIndices = EGraphics.getColorIndices();
						for(int i=0; i<colorIndices.length; i++)
				            co.addItem(EGraphics.getColorIndexName(colorIndices[i]));
						addChangePossibility("Text Color:", currentChangeValues[c] = co, null, onePanel);
						break;
					case CHANGEBOLD:
						JComboBox bd = new JComboBox();
						bd.addItem("Leave alone");   bd.addItem("Make Bold");   bd.addItem("Make Not Bold");
						addChangePossibility("Bold:", currentChangeValues[c] = bd, null, onePanel);
						break;
					case CHANGEITALIC:
						JComboBox it = new JComboBox();
						it.addItem("Leave alone");   it.addItem("Make Italic");   it.addItem("Make Not Italic");
						addChangePossibility("Italic:", currentChangeValues[c] = it, null, onePanel);
						break;
					case CHANGEUNDERLINE:
						JComboBox ul = new JComboBox();
						ul.addItem("Leave alone");   ul.addItem("Make Underlined");   ul.addItem("Make Not Underlined");
						addChangePossibility("Underlined:", currentChangeValues[c] = ul, null, onePanel);
						break;
					case CHANGECODE:
						JComboBox cd = new JComboBox();
						cd.addItem("Leave alone");
						for (Iterator<TextDescriptor.Code> cIt = TextDescriptor.Code.getCodes(); cIt.hasNext(); )
			                cd.addItem(cIt.next());
						addChangePossibility("Code:", currentChangeValues[c] = cd, null, onePanel);
						break;
					case CHANGEUNITS:
						JComboBox un = new JComboBox();
						un.addItem("Leave alone");
			            for (Iterator<TextDescriptor.Unit> uIt = TextDescriptor.Unit.getUnits(); uIt.hasNext(); )
							un.addItem(uIt.next());
						addChangePossibility("Units:", currentChangeValues[c] = un, null, onePanel);
						break;
					case CHANGESHOW:
						JComboBox sh = new JComboBox();
						sh.addItem("Leave alone");
			            for (Iterator<TextDescriptor.DispPos> sIt = TextDescriptor.DispPos.getShowStyles(); sIt.hasNext(); )
							sh.addItem(sIt.next());
						addChangePossibility("Show:", currentChangeValues[c] = sh, null, onePanel);
						break;
				}
				changePanel.add(onePanel);
			}
		}
	}

	private void addChangePossibility(String label, JComponent comp, String msg, JPanel onePanel)
	{
		int bottom = 4;
		if (msg != null) bottom = 0;
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
        gbc.insets = new Insets(4, 4, bottom, 4);
		onePanel.add(new JLabel(label), gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, bottom, 4);
		onePanel.add(comp, gbc);

		if (msg != null)
		{
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;   gbc.gridwidth = 2;
	        gbc.insets = new Insets(0, 4, 4, 4);
			onePanel.add(new JLabel(msg), gbc);
		}
	}

	private static class SortMultipleHighlights implements Comparator<Highlight2>
	{
		public int compare(Highlight2 h1, Highlight2 h2)
		{
			// if the types are different, order by types
			if (h1.getClass() != h2.getClass())
			{
                return h1.getClass().hashCode() - h2.getClass().hashCode();
			}

			// if not a geometric, no order is available
			if (!h1.isHighlightEOBJ()) return 0;

			// sort on mix of NodeInst / ArcInst / PortInst
			ElectricObject e1 = h1.getElectricObject();
			int type1 = 0;
			if (e1 instanceof NodeInst) type1 = 1; else
				if (e1 instanceof ArcInst) type1 = 2;
			ElectricObject e2 = h2.getElectricObject();
			int type2 = 0;
			if (e2 instanceof NodeInst) type2 = 1; else
				if (e2 instanceof ArcInst) type2 = 2;
			if (type1 != type2) return type1 - type2;

			// sort on the object name
			String s1 = null, s2 = null;
			if (e1 instanceof Geometric) s1 = ((Geometric)e1).describe(false); else
				s1 = e1.toString();
			if (e2 instanceof Geometric) s2 = ((Geometric)e2).describe(false); else
				s2 = e2.toString();
			return TextUtils.canonicString(s1).compareTo(TextUtils.canonicString(s2));
		}
	}
	
	private JComponent findComponentRawValue(int type)
	{
		for(int c=0; c<currentChangeTypes.length; c++)
		{
			int change = currentChangeTypes[c];
			if (change == type) return currentChangeValues[c];
		}
		return null;
	}
	
	private String findComponentStringValue(int type)
	{
		for(int c=0; c<currentChangeTypes.length; c++)
		{
			int change = currentChangeTypes[c];
			if (change == type) return ((JTextField)currentChangeValues[c]).getText().trim();
		}
		return "";
	}
	
	private int findComponentIntValue(int type)
	{
		for(int c=0; c<currentChangeTypes.length; c++)
		{
			int change = currentChangeTypes[c];
			if (change == type) return ((JComboBox)currentChangeValues[c]).getSelectedIndex();
		}
		return 0;
	}

	/**
	 * This class implements database changes requested by the dialog.
	 */
	private static class MultiChange extends Job
	{
		GetInfoMulti dialog;

		protected MultiChange(GetInfoMulti dialog)
		{
			super("Modify Objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			// change nodes
			int numNodes = dialog.nodeList.size();
			if (numNodes > 0)
			{
				String xPos = dialog.findComponentStringValue(CHANGEXPOS);
				String yPos = dialog.findComponentStringValue(CHANGEYPOS);
				String xSize = dialog.findComponentStringValue(CHANGEXSIZE);
				String ySize = dialog.findComponentStringValue(CHANGEYSIZE);
				String rot = dialog.findComponentStringValue(CHANGEROTATION);
				int lr = dialog.findComponentIntValue(CHANGEMIRRORLR);
				int ud = dialog.findComponentIntValue(CHANGEMIRRORUD);
				int expanded = dialog.findComponentIntValue(CHANGEEXPANDED);
				int easySelect = dialog.findComponentIntValue(CHANGEEASYSELECT);
				int invisOutside = dialog.findComponentIntValue(CHANGEINVOUTSIDECELL);
				int locked = dialog.findComponentIntValue(CHANGELOCKED);				

				// make other node changes
				boolean changes = false;
				for(NodeInst ni : dialog.nodeList)
				{
					if (ni.getProto() instanceof Cell)
					{
						if (expanded == 1)
						{
							ni.setExpanded();
							changes = true;
						} else if (expanded == 2)
						{
							ni.clearExpanded();
							changes = true;
						}
					}
					if (easySelect == 1) ni.clearHardSelect(); else
						if (easySelect == 2) ni.setHardSelect();
					if (invisOutside == 1) ni.setVisInside(); else
						if (invisOutside == 2) ni.clearVisInside();
					if (locked == 1) ni.setLocked(); else
						if (locked == 2) ni.clearLocked();	
				}

				// see if size, position, or orientation changed
				if (xPos.length() > 0 || yPos.length() > 0 || xSize.length() > 0 || ySize.length() > 0 ||
					rot.length() > 0 || lr != 0 || ud != 0 || changes)
				{
					// can do mass changes, but not orientation
					if (rot.length() == 0 && lr == 0 && ud == 0)
					{
						// change all nodes
						NodeInst [] nis = new NodeInst[numNodes];
						double [] dXP = new double[numNodes];
						double [] dYP = new double[numNodes];
						double [] dXS = new double[numNodes];
						double [] dYS = new double[numNodes];
						double newXPosition = TextUtils.atof(xPos);
						double newYPosition = TextUtils.atof(yPos);
						int i = 0;
						for(NodeInst ni : dialog.nodeList)
						{
		                    SizeOffset so = ni.getSizeOffset();
							nis[i] = ni;
							if (xPos.length() == 0) dXP[i] = 0; else
								dXP[i] = newXPosition - ni.getAnchorCenterX();
							if (yPos.equals("")) dYP[i] = 0; else
								dYP[i] = newYPosition - ni.getAnchorCenterY();
							String newXSize = xSize;
							String newYSize = ySize;
					        if (ni.getAngle() == 900 || ni.getAngle() == 2700)
							{
								String swap = newXSize;   newXSize = newYSize;   newYSize = swap;
							}
							if (newXSize.equals("")) dXS[i] = 0; else
							{
								double trueXSize = TextUtils.atof(newXSize) + so.getHighXOffset() + so.getLowXOffset();
								dXS[i] = trueXSize - ni.getXSize();
							}
							if (newYSize.equals("")) dYS[i] = 0; else
							{
								double trueYSize = TextUtils.atof(newYSize) + so.getHighYOffset() + so.getLowYOffset();
								dYS[i] = trueYSize - ni.getYSize();
							}
							i++;
						}
						NodeInst.modifyInstances(nis, dXP, dYP, dXS, dYS);
					} else
					{
						for(NodeInst ni : dialog.nodeList)
						{
		                    SizeOffset so = ni.getSizeOffset();
							double dX = 0, dY = 0, dXS = 0, dYS = 0;
							if (xPos.length() > 0) dX = TextUtils.atof(xPos) - ni.getAnchorCenterX();
							if (yPos.length() > 0) dY = TextUtils.atof(yPos) - ni.getAnchorCenterY();
							String newXSize = xSize;
							String newYSize = ySize;
					        if (ni.getAngle() == 900 || ni.getAngle() == 2700)
							{
								String swap = newXSize;   newXSize = newYSize;   newYSize = swap;
							}
							if (newXSize.length() > 0)
							{
								double trueXSize = TextUtils.atof(newXSize) + so.getHighXOffset() + so.getLowXOffset();
								dXS = trueXSize - ni.getXSize();
							}
							if (newYSize.length() > 0)
							{
								double trueYSize = TextUtils.atof(newYSize) + so.getHighYOffset() + so.getLowYOffset();
								dYS = trueYSize - ni.getYSize();
							}
							int dRot = 0;
							if (rot.length() > 0) dRot = (TextUtils.atoi(rot) - ni.getAngle() + 3600) % 3600;
							boolean dMirrorLR = false;
							if (lr == 1 && !ni.isXMirrored()) dMirrorLR = true; else
								if (lr == 2 && ni.isXMirrored()) dMirrorLR = true;
							boolean dMirrorUD = false;
							if (ud == 1 && !ni.isYMirrored()) dMirrorUD = true; else
								if (ud == 2 && ni.isYMirrored()) dMirrorUD = true;
			                Orientation orient = Orientation.fromJava(dRot, dMirrorLR, dMirrorUD);
							ni.modifyInstance(dX, dY, dXS, dYS, orient);
						}
					}
				}
			}

			if (dialog.arcList.size() > 0)
			{
				String width = dialog.findComponentStringValue(CHANGEWIDTH);
				int rigid = dialog.findComponentIntValue(CHANGERIGID);
				int fixedangle = dialog.findComponentIntValue(CHANGEFIXANGLE);
				int slidable = dialog.findComponentIntValue(CHANGESLIDABLE);
				int extension = dialog.findComponentIntValue(CHANGEEXTENSION);
				int directional = dialog.findComponentIntValue(CHANGEDIRECTION);
				int negated = dialog.findComponentIntValue(CHANGENEGATION);
				int easySelect = dialog.findComponentIntValue(CHANGEEASYSELECT);

				for(ArcInst ai : dialog.arcList)
				{
					if (width.length() > 0)
					{
						double newWidth = TextUtils.atof(width) + ai.getProto().getWidthOffset();
						if (newWidth != ai.getWidth())
							ai.modify(newWidth - ai.getWidth(), 0, 0, 0, 0);
					}
					if (rigid == 1) ai.setRigid(true); else
						if (rigid == 2) ai.setRigid(false);
					if (fixedangle == 1) ai.setFixedAngle(true); else
						if (fixedangle == 2) ai.setFixedAngle(false);
					if (slidable == 1) ai.setSlidable(true); else
						if (slidable == 2) ai.setSlidable(false);
					switch (extension)
					{
						case 1: ai.setExtended(ArcInst.HEADEND, true);    ai.setExtended(ArcInst.TAILEND, true);    break;
						case 2: ai.setExtended(ArcInst.HEADEND, false);   ai.setExtended(ArcInst.TAILEND, false);   break;
						case 3: ai.setExtended(ArcInst.HEADEND, true);    ai.setExtended(ArcInst.TAILEND, false);   break;
						case 4: ai.setExtended(ArcInst.HEADEND, false);   ai.setExtended(ArcInst.TAILEND, true);    break;
					}
					switch (directional)
					{
						case 1: ai.setArrowed(ArcInst.HEADEND, false);   ai.setArrowed(ArcInst.TAILEND, false);
							ai.setBodyArrowed(false);   break;
						case 2: ai.setArrowed(ArcInst.HEADEND, true);    ai.setArrowed(ArcInst.TAILEND, false);
							ai.setBodyArrowed(true);   break;
						case 3: ai.setArrowed(ArcInst.HEADEND, false);   ai.setArrowed(ArcInst.TAILEND, true);
							ai.setBodyArrowed(true);    break;
						case 4: ai.setArrowed(ArcInst.HEADEND, false);   ai.setArrowed(ArcInst.TAILEND, false);
							ai.setBodyArrowed(true);    break;
						case 5: ai.setArrowed(ArcInst.HEADEND, true);    ai.setArrowed(ArcInst.TAILEND, true);
							ai.setBodyArrowed(true);    break;
					}
					switch (negated)
					{
						case 1: ai.setNegated(ArcInst.HEADEND, false);   ai.setNegated(ArcInst.TAILEND, false);   break;
						case 2: ai.setNegated(ArcInst.HEADEND, true);    ai.setNegated(ArcInst.TAILEND, false);   break;
						case 3: ai.setNegated(ArcInst.HEADEND, false);   ai.setNegated(ArcInst.TAILEND, true);    break;
						case 4: ai.setNegated(ArcInst.HEADEND, true);    ai.setNegated(ArcInst.TAILEND, true);    break;
					}
					if (easySelect == 1) ai.setHardSelect(false); else
						if (easySelect == 2) ai.setHardSelect(true);
				}
			}

			if (dialog.exportList.size() > 0)
			{
				int characteristics = dialog.findComponentIntValue(CHANGECHARACTERISTICS);
				int bodyOnly = dialog.findComponentIntValue(CHANGEBODYONLY);
				int alwaysDrawn = dialog.findComponentIntValue(CHANGEALWAYSDRAWN);
				String pointSize = dialog.findComponentStringValue(CHANGEPOINTSIZE);
				String unitSize = dialog.findComponentStringValue(CHANGEUNITSIZE);
				String xOff = dialog.findComponentStringValue(CHANGEXOFF);
				String yOff = dialog.findComponentStringValue(CHANGEYOFF);
				int textRotation = dialog.findComponentIntValue(CHANGETEXTROT);
				int anchor = dialog.findComponentIntValue(CHANGEANCHOR);
				int font = dialog.findComponentIntValue(CHANGEFONT);
				int color = dialog.findComponentIntValue(CHANGECOLOR);
				int bold = dialog.findComponentIntValue(CHANGEBOLD);
				int italic = dialog.findComponentIntValue(CHANGEITALIC);
				int underline = dialog.findComponentIntValue(CHANGEUNDERLINE);
				int invisOutside = dialog.findComponentIntValue(CHANGEINVOUTSIDECELL);

				for(Export e : dialog.exportList)
				{
					if (characteristics != 0)
					{
						JComboBox chBox = (JComboBox)dialog.findComponentRawValue(CHANGECHARACTERISTICS);
						String charName = (String)chBox.getSelectedItem();
						PortCharacteristic ch = PortCharacteristic.findCharacteristic(charName);
						e.setCharacteristic(ch);
					}
					if (bodyOnly == 1) e.setBodyOnly(true); else
						if (bodyOnly == 2) e.setBodyOnly(false);
					if (alwaysDrawn == 1) e.setAlwaysDrawn(true); else
						if (alwaysDrawn == 2) e.setAlwaysDrawn(false);

					MutableTextDescriptor td = e.getMutableTextDescriptor(Export.EXPORT_NAME);
					boolean tdChanged = false;
					if (pointSize.length() > 0)
					{
						td.setAbsSize(TextUtils.atoi(pointSize));
						tdChanged = true;
					}
					if (unitSize.length() > 0)
					{
						td.setRelSize(TextUtils.atof(unitSize));
						tdChanged = true;
					}
					if (xOff.length() > 0)
					{
						td.setOff(TextUtils.atof(xOff), td.getYOff());
						tdChanged = true;
					}
					if (yOff.length() > 0)
					{
						td.setOff(td.getXOff(), TextUtils.atof(yOff));
						tdChanged = true;
					}
					if (textRotation > 0)
					{
						switch (textRotation)
						{
							case 1: td.setRotation(TextDescriptor.Rotation.ROT0);     break;
							case 2: td.setRotation(TextDescriptor.Rotation.ROT90);    break;
							case 3: td.setRotation(TextDescriptor.Rotation.ROT180);   break;
							case 4: td.setRotation(TextDescriptor.Rotation.ROT270);   break;
						}
						tdChanged = true;
					}
					if (anchor > 0)
					{
						JComboBox anBox = (JComboBox)dialog.findComponentRawValue(CHANGEANCHOR);
				        TextDescriptor.Position newPosition = (TextDescriptor.Position)anBox.getSelectedItem();
						td.setPos(newPosition);
						tdChanged = true;
					}
					if (font > 0)
					{
						JComboBox foBox = (JComboBox)dialog.findComponentRawValue(CHANGEFONT);
						String fontName = (String)foBox.getSelectedItem();
		                TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(fontName);
		                int newFontIndex = newFont != null ? newFont.getIndex() : 0;
		                td.setFace(newFontIndex);
						tdChanged = true;
					}
					if (color > 0)
					{
						JComboBox coBox = (JComboBox)dialog.findComponentRawValue(CHANGECOLOR);
						int [] colorIndices = EGraphics.getColorIndices();
				        int newColorComboIndex = coBox.getSelectedIndex();
				        int newColorIndex = colorIndices[newColorComboIndex-1];
						td.setColorIndex(newColorIndex);
						tdChanged = true;
					}
					if (bold == 1) { td.setBold(true);   tdChanged = true; } else
						if (bold == 2) { td.setBold(false);   tdChanged = true; }
					if (italic == 1) { td.setItalic(true);   tdChanged = true; } else
						if (italic == 2) { td.setItalic(false);   tdChanged = true; }
					if (underline == 1) { td.setUnderline(true);   tdChanged = true; } else
						if (underline == 2) { td.setUnderline(false);   tdChanged = true; }
					if (invisOutside == 1) { td.setInterior(true);   tdChanged = true; } else
						if (invisOutside == 2) { td.setInterior(false);   tdChanged = true; }

					// update text descriptor if it changed
					if (tdChanged)
						e.setTextDescriptor(Export.EXPORT_NAME, TextDescriptor.newTextDescriptor(td));
				}
			}

			if (dialog.textList.size() > 0)
			{
				for(Highlight2 h : dialog.textList)
				{
					ElectricObject eobj = h.getElectricObject();
					if (!h.isHighlightText()) continue;

					Variable.Key descKey = null;
					if (h.getVar() != null)
					{
						descKey = h.getVar().getKey();
					} else
					{
						if (h.getName() != null)
						{
							if (eobj instanceof NodeInst) descKey = NodeInst.NODE_NAME; else
								if (eobj instanceof ArcInst) descKey = ArcInst.ARC_NAME;
						} else if (eobj instanceof NodeInst)
						{
							descKey = NodeInst.NODE_PROTO;
						}
					}
					if (descKey == null) continue;
					MutableTextDescriptor td = eobj.getMutableTextDescriptor(descKey);

					String pointSize = dialog.findComponentStringValue(CHANGEPOINTSIZE);
					String unitSize = dialog.findComponentStringValue(CHANGEUNITSIZE);
					String xOff = dialog.findComponentStringValue(CHANGEXOFF);
					String yOff = dialog.findComponentStringValue(CHANGEYOFF);
					int textRotation = dialog.findComponentIntValue(CHANGETEXTROT);
					int anchor = dialog.findComponentIntValue(CHANGEANCHOR);
					int font = dialog.findComponentIntValue(CHANGEFONT);
					int color = dialog.findComponentIntValue(CHANGECOLOR);
					int code = dialog.findComponentIntValue(CHANGECODE);
					int units = dialog.findComponentIntValue(CHANGEUNITS);
					int show = dialog.findComponentIntValue(CHANGESHOW);
					int bold = dialog.findComponentIntValue(CHANGEBOLD);
					int italic = dialog.findComponentIntValue(CHANGEITALIC);
					int underline = dialog.findComponentIntValue(CHANGEUNDERLINE);
					int invisOutside = dialog.findComponentIntValue(CHANGEINVOUTSIDECELL);

					boolean tdChanged = false;
					if (pointSize.length() > 0)
					{
						td.setAbsSize(TextUtils.atoi(pointSize));
						tdChanged = true;
					}
					if (unitSize.length() > 0)
					{
						td.setRelSize(TextUtils.atof(unitSize));
						tdChanged = true;
					}
					if (xOff.length() > 0)
					{
						td.setOff(TextUtils.atof(xOff), td.getYOff());
						tdChanged = true;
					}
					if (yOff.length() > 0)
					{
						td.setOff(td.getXOff(), TextUtils.atof(yOff));
						tdChanged = true;
					}
					if (textRotation > 0)
					{
						switch (textRotation)
						{
							case 1: td.setRotation(TextDescriptor.Rotation.ROT0);     break;
							case 2: td.setRotation(TextDescriptor.Rotation.ROT90);    break;
							case 3: td.setRotation(TextDescriptor.Rotation.ROT180);   break;
							case 4: td.setRotation(TextDescriptor.Rotation.ROT270);   break;
						}
						tdChanged = true;
					}
					if (anchor > 0)
					{
						JComboBox anBox = (JComboBox)dialog.findComponentRawValue(CHANGEANCHOR);
				        TextDescriptor.Position newPosition = (TextDescriptor.Position)anBox.getSelectedItem();
						td.setPos(newPosition);
						tdChanged = true;
					}
					if (font > 0)
					{
						JComboBox foBox = (JComboBox)dialog.findComponentRawValue(CHANGEFONT);
						String fontName = (String)foBox.getSelectedItem();
		                TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(fontName);
		                int newFontIndex = newFont != null ? newFont.getIndex() : 0;
		                td.setFace(newFontIndex);
						tdChanged = true;
					}
					if (color > 0)
					{
						JComboBox coBox = (JComboBox)dialog.findComponentRawValue(CHANGECOLOR);
						int [] colorIndices = EGraphics.getColorIndices();
				        int newColorComboIndex = coBox.getSelectedIndex();
				        int newColorIndex = colorIndices[newColorComboIndex-1];
						td.setColorIndex(newColorIndex);
						tdChanged = true;
					}
					if (code > 0)
					{
						JComboBox cdBox = (JComboBox)dialog.findComponentRawValue(CHANGECODE);
						TextDescriptor.Code cd = (TextDescriptor.Code)cdBox.getSelectedItem();
						td.setCode(cd);
						tdChanged = true;
					}
					if (units > 0)
					{
						JComboBox unBox = (JComboBox)dialog.findComponentRawValue(CHANGEUNITS);
						TextDescriptor.Unit un = (TextDescriptor.Unit)unBox.getSelectedItem();
						td.setUnit(un);
						tdChanged = true;
					}
					if (show > 0)
					{
						JComboBox shBox = (JComboBox)dialog.findComponentRawValue(CHANGESHOW);
						TextDescriptor.DispPos sh = (TextDescriptor.DispPos)shBox.getSelectedItem();
						td.setDispPart(sh);
						tdChanged = true;
					}					
					if (bold == 1) { td.setBold(true);   tdChanged = true; } else
						if (bold == 2) { td.setBold(false);   tdChanged = true; }
					if (italic == 1) { td.setItalic(true);   tdChanged = true; } else
						if (italic == 2) { td.setItalic(false);   tdChanged = true; }
					if (underline == 1) { td.setUnderline(true);   tdChanged = true; } else
						if (underline == 2) { td.setUnderline(false);   tdChanged = true; }
					if (invisOutside == 1) { td.setInterior(true);   tdChanged = true; } else
						if (invisOutside == 2) { td.setInterior(false);   tdChanged = true; }

					// update text descriptor if it changed
					if (tdChanged)
						eobj.setTextDescriptor(descKey, TextDescriptor.newTextDescriptor(td));
				}
			}
			return true;
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

        removeOthers = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        selectionCount = new javax.swing.JLabel();
        listPane = new javax.swing.JScrollPane();
        ok = new javax.swing.JButton();
        remove = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        possibleChanges = new javax.swing.JScrollPane();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Multi-Object Properties");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        removeOthers.setText("Remove Others");
        removeOthers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeOthersActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.33;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(removeOthers, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.33;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        selectionCount.setText("0 selections:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        getContentPane().add(selectionCount, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(300, 200));
        listPane.setPreferredSize(new java.awt.Dimension(300, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(listPane, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        remove.setText("Remove");
        remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.33;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(remove, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        getContentPane().add(cancel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(possibleChanges, gridBagConstraints);

        pack();
    }
    // </editor-fold>//GEN-END:initComponents

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		applyActionPerformed(evt);
		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed

	private void removeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removeActionPerformed
	{//GEN-HEADEREND:event_removeActionPerformed
		int [] items = list.getSelectedIndices();
		List<Integer> indices = new ArrayList<Integer>();
		for(int i=0; i<items.length; i++) indices.add(new Integer(items[i]));
		Collections.sort(indices, new Comparator<Integer>()
		{
			public int compare(Integer c1, Integer c2) { return c2.compareTo(c1); }
		});
		for(Integer index : indices)
		{
			highlightList.remove(index.intValue());
		}
        if (wnd != null) {
            Highlighter highlighter = wnd.getHighlighter();
            highlighter.clear();
            highlighter.setHighlightList(highlightList);
            highlighter.finished();
        }
	}//GEN-LAST:event_removeActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		MultiChange job = new MultiChange(this);
	}//GEN-LAST:event_applyActionPerformed

	private void removeOthersActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removeOthersActionPerformed
	{//GEN-HEADEREND:event_removeOthersActionPerformed
		int [] items = list.getSelectedIndices();
		Set<Integer> keepIndices = new HashSet<Integer>();
		for(int i=0; i<items.length; i++) keepIndices.add(new Integer(items[i]));
		int len = highlightList.size();
		for(int i=len-1; i>=0; i--)
		{
			if (keepIndices.contains(new Integer(i))) continue;
			highlightList.remove(i);
		}
        if (wnd != null) {
            Highlighter highlighter = wnd.getHighlighter();
            highlighter.clear();
            highlighter.setHighlightList(highlightList);
            highlighter.finished();
        }
	}//GEN-LAST:event_removeOthersActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);

		// clear the list of highlights so that this dialog doesn't trap memory
		highlightList.clear();
//		theDialog = null;
//		if (wnd != null) wnd.getHighlighter().removeHighlightListener(this);
//		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JButton cancel;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JButton ok;
    private javax.swing.JScrollPane possibleChanges;
    private javax.swing.JButton remove;
    private javax.swing.JButton removeOthers;
    private javax.swing.JLabel selectionCount;
    // End of variables declaration//GEN-END:variables

}
