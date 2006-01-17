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
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
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
	List<DisplayedText> textList;

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
		textList = new ArrayList<DisplayedText>();
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
				Variable.Key varKey = h.getVarKey();
				if (varKey != null)
					textList.add(new DisplayedText(eobj, varKey));
			}
		}

		// with exactly 2 objects, show the distance between them
		if (nodeList.size() + arcList.size() == 2)
		{
			displayList.add("---------------------------");
			Point2D firstPt = firstGeom.getTrueCenter();
			if (firstGeom instanceof NodeInst) firstPt = ((NodeInst)firstGeom).getAnchorCenter();
			Point2D secondPt = secondGeom.getTrueCenter();
			if (secondGeom instanceof NodeInst) secondPt = ((NodeInst)secondGeom).getAnchorCenter();
			displayList.add("Distance between centers: X=" + Math.abs(firstPt.getX() - secondPt.getX()) +
			   " Y=" + Math.abs(firstPt.getY() - secondPt.getY()));
		}

		// reload the list (much more efficient than clearing and reloading if it is already displayed)
		list.setListData(displayList.toArray());

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
		private String xPos, yPos;
		private String xSize, ySize;
		private String rot;
		private int lr, ud;
		private int expanded;
		private int easySelect;
		private int invisOutside;
		private int locked;
		private String width;
		private int rigid, fixedangle, slidable;
		private int extension, directional, negated;
		private String characteristics;
		private int bodyOnly;
		private int alwaysDrawn;
		private String pointSize, unitSize;
		private String xOff, yOff;
		private int textRotation;
		private int anchor;
		private String font;
		private int color;
		private int bold, italic, underline;
		private int code;
		private int units;
		private int show;
		private List<NodeInst> nodeList;
		private List<ArcInst> arcList;
		private List<Export> exportList;
		private List<DisplayedText> textList;

		public MultiChange() {}

		protected MultiChange(
			String xPos,
			String yPos,
			String xSize,
			String ySize,
			String rot,
			int lr,
			int ud,
			int expanded,
			int easySelect,
			int invisOutside,
			int locked,
			String width,
			int rigid,
			int fixedangle,
			int slidable,
			int extension,
			int directional,
			int negated,
			String characteristics,
			int bodyOnly,
			int alwaysDrawn,
			String pointSize,
			String unitSize,
			String xOff,
			String yOff,
			int textRotation,
			int anchor,
			String font,
			int color,
			int bold,
			int italic,
			int underline,
			int code,
			int units,
			int show,
			List<NodeInst> nodeList, List<ArcInst> arcList,
			List<Export> exportList, List<DisplayedText> textList)
		{
			super("Modify Objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.xPos = xPos;
			this.yPos = yPos;
			this.xSize = xSize;
			this.ySize = ySize;
			this.rot = rot;
			this.lr = lr;
			this.ud = ud;
			this.expanded = expanded;
			this.easySelect = easySelect;
			this.invisOutside = invisOutside;
			this.locked = locked;
			this.width = width;
			this.rigid = rigid;
			this.fixedangle = fixedangle;
			this.slidable = slidable;
			this.extension = extension;
			this.directional = directional;
			this.negated = negated;
			this.characteristics = characteristics;
			this.bodyOnly = bodyOnly;
			this.alwaysDrawn = alwaysDrawn;
			this.pointSize = pointSize;
			this.unitSize = unitSize;
			this.xOff = xOff;
			this.yOff = yOff;
			this.textRotation = textRotation;
			this.anchor = anchor;
			this.font = font;
			this.color = color;
			this.bold = bold;
			this.italic = italic;
			this.underline = underline;
			this.code = code;
			this.units = units;
			this.show = show;
			this.nodeList = nodeList;
			this.arcList = arcList;
			this.exportList = exportList;
			this.textList = textList;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// change nodes
			int numNodes = nodeList.size();
			if (numNodes > 0)
			{
				// make other node changes
				boolean changes = false;
				for(NodeInst ni : nodeList)
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
						for(NodeInst ni : nodeList)
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
						for(NodeInst ni : nodeList)
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
							if (rot.length() > 0) dRot = ((int)(TextUtils.atof(rot)*10) - ni.getAngle() + 3600) % 3600;
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

			if (arcList.size() > 0)
			{
				for(ArcInst ai : arcList)
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

			if (exportList.size() > 0)
			{
				for(Export e : exportList)
				{
					if (characteristics != null)
					{
						PortCharacteristic ch = PortCharacteristic.findCharacteristic(characteristics);
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
					if (anchor >= 0)
					{
				        TextDescriptor.Position newPosition = TextDescriptor.Position.getPositionAt(anchor);
						td.setPos(newPosition);
						tdChanged = true;
					}
					if (font != null)
					{
		                TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(font);
		                int newFontIndex = newFont != null ? newFont.getIndex() : 0;
		                td.setFace(newFontIndex);
						tdChanged = true;
					}
					if (color > 0)
					{
						int [] colorIndices = EGraphics.getColorIndices();
				        int newColorIndex = colorIndices[color-1];
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

			if (textList.size() > 0)
			{
				for(DisplayedText dt : textList)
				{
					ElectricObject eobj = dt.getElectricObject();
					Variable.Key descKey = dt.getVariableKey();
					MutableTextDescriptor td = eobj.getMutableTextDescriptor(descKey);

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
					if (anchor >= 0)
					{
				        TextDescriptor.Position newPosition = TextDescriptor.Position.getPositionAt(anchor);
						td.setPos(newPosition);
						tdChanged = true;
					}
					if (font != null)
					{
		                TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(font);
		                int newFontIndex = newFont != null ? newFont.getIndex() : 0;
		                td.setFace(newFontIndex);
						tdChanged = true;
					}
					if (color > 0)
					{
						int [] colorIndices = EGraphics.getColorIndices();
				        int newColorIndex = colorIndices[color-1];
						td.setColorIndex(newColorIndex);
						tdChanged = true;
					}
					if (code > 0)
					{
						TextDescriptor.Code cd = TextDescriptor.Code.getByCBits(code);
						td.setCode(cd);
						tdChanged = true;
					}
					if (units > 0)
					{
						TextDescriptor.Unit un = TextDescriptor.Unit.getUnitAt(units);
						td.setUnit(un);
						tdChanged = true;
					}
					if (show > 0)
					{
						TextDescriptor.DispPos sh = TextDescriptor.DispPos.getShowStylesAt(show);
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
		// change nodes
		String xPos = null;
		String yPos = null;
		String xSize = null;
		String ySize = null;
		String rot = null;
		int lr = 0;
		int ud = 0;
		int expanded = 0;
		int easySelect = 0;
		int invisOutside = 0;
		int locked = 0;
		String width = null;
		int rigid = 0;
		int fixedangle = 0;
		int slidable = 0;
		int extension = 0;
		int directional = 0;
		int negated = 0;
		String characteristics = null;
		int bodyOnly = 0;
		int alwaysDrawn = 0;
		String pointSize = null;
		String unitSize = null;
		String xOff = null;
		String yOff = null;
		int textRotation = 0;
		int anchor = -1;
		String font = null;
		int color = 0;
		int bold = 0;
		int italic = 0;
		int underline = 0;
		int code = -1;
		int units = -1;
		int show = -1;
		if (nodeList.size() > 0)
		{
			xPos = findComponentStringValue(CHANGEXPOS);
			yPos = findComponentStringValue(CHANGEYPOS);
			xSize = findComponentStringValue(CHANGEXSIZE);
			ySize = findComponentStringValue(CHANGEYSIZE);
			rot = findComponentStringValue(CHANGEROTATION);
			lr = findComponentIntValue(CHANGEMIRRORLR);
			ud = findComponentIntValue(CHANGEMIRRORUD);
			expanded = findComponentIntValue(CHANGEEXPANDED);
			easySelect = findComponentIntValue(CHANGEEASYSELECT);
			invisOutside = findComponentIntValue(CHANGEINVOUTSIDECELL);
			locked = findComponentIntValue(CHANGELOCKED);
		}
		if (arcList.size() > 0)
		{
			width = findComponentStringValue(CHANGEWIDTH);
			rigid = findComponentIntValue(CHANGERIGID);
			fixedangle = findComponentIntValue(CHANGEFIXANGLE);
			slidable = findComponentIntValue(CHANGESLIDABLE);
			extension = findComponentIntValue(CHANGEEXTENSION);
			directional = findComponentIntValue(CHANGEDIRECTION);
			negated = findComponentIntValue(CHANGENEGATION);
			easySelect = findComponentIntValue(CHANGEEASYSELECT);
		}
		if (exportList.size() > 0)
		{
			characteristics = (String)((JComboBox)findComponentRawValue(CHANGECHARACTERISTICS)).getSelectedItem();
			bodyOnly = findComponentIntValue(CHANGEBODYONLY);
			alwaysDrawn = findComponentIntValue(CHANGEALWAYSDRAWN);
			pointSize = findComponentStringValue(CHANGEPOINTSIZE);
			unitSize = findComponentStringValue(CHANGEUNITSIZE);
			xOff = findComponentStringValue(CHANGEXOFF);
			yOff = findComponentStringValue(CHANGEYOFF);
			textRotation = findComponentIntValue(CHANGETEXTROT);
			Object anValue = ((JComboBox)findComponentRawValue(CHANGEANCHOR)).getSelectedItem();
			if (anValue instanceof TextDescriptor.Position)
				anchor = ((TextDescriptor.Position)anValue).getIndex();
			font = (String)((JComboBox)findComponentRawValue(CHANGEFONT)).getSelectedItem();
			color = ((JComboBox)findComponentRawValue(CHANGECOLOR)).getSelectedIndex();
			bold = findComponentIntValue(CHANGEBOLD);
			italic = findComponentIntValue(CHANGEITALIC);
			underline = findComponentIntValue(CHANGEUNDERLINE);
			invisOutside = findComponentIntValue(CHANGEINVOUTSIDECELL);
		}
		if (textList.size() > 0)
		{
			pointSize = findComponentStringValue(CHANGEPOINTSIZE);
			unitSize = findComponentStringValue(CHANGEUNITSIZE);
			xOff = findComponentStringValue(CHANGEXOFF);
			yOff = findComponentStringValue(CHANGEYOFF);
			textRotation = findComponentIntValue(CHANGETEXTROT);
			Object anValue = ((JComboBox)findComponentRawValue(CHANGEANCHOR)).getSelectedItem();
			if (anValue instanceof TextDescriptor.Position)
				anchor = ((TextDescriptor.Position)anValue).getIndex();
			font = (String)((JComboBox)findComponentRawValue(CHANGEFONT)).getSelectedItem();
			color = ((JComboBox)findComponentRawValue(CHANGECOLOR)).getSelectedIndex();
			Object cdValue = ((JComboBox)findComponentRawValue(CHANGECODE)).getSelectedItem();
			if (cdValue instanceof TextDescriptor.Code)
				code = ((TextDescriptor.Code)cdValue).getCFlags();
			Object unValue = ((JComboBox)findComponentRawValue(CHANGEUNITS)).getSelectedItem();
			if (unValue instanceof TextDescriptor.Unit)
				units = ((TextDescriptor.Unit)unValue).getIndex();
			Object shValue = ((JComboBox)findComponentRawValue(CHANGESHOW)).getSelectedItem();
			if (shValue instanceof TextDescriptor.DispPos)
				show = ((TextDescriptor.DispPos)shValue).getIndex();
			bold = findComponentIntValue(CHANGEBOLD);
			italic = findComponentIntValue(CHANGEITALIC);
			underline = findComponentIntValue(CHANGEUNDERLINE);
			invisOutside = findComponentIntValue(CHANGEINVOUTSIDECELL);
		}

		new MultiChange(xPos, yPos, xSize, ySize, rot, lr, ud, expanded, easySelect, invisOutside,
			locked, width, rigid, fixedangle, slidable, extension, directional, negated,
			characteristics, bodyOnly, alwaysDrawn, pointSize, unitSize, xOff, yOff, textRotation,
			anchor, font, color, bold, italic, underline, code, units, show, nodeList, arcList, exportList, textList);
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
