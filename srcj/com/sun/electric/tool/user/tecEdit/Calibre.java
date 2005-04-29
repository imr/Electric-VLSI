/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Calibre.java
 * Technology Editor, read a Calibre file
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.PromptAt;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This class reads a Calibre file into Electric.
 * Calibre uses Mentor's "Standard Verification Rule Format" (SVRF) language.
 */
public class Calibre
{
	private static final int MAXDEPTH = 10;

	private static class Rule
	{
		String name;
		String comment;
		List lines;
	}

	private static class Layer
	{
		String name;
		int gdsNumber;
		String comment;
		boolean ignore;
		boolean derived;
		List derivedRule;
	}

	private LineNumberReader lineReader;
	private HashMap allVariables;
	private HashMap allRules;
	private HashMap allLayers;
	private HashSet allDefines;
	private String curLine;
	private double precision, resolution;
	private boolean [] preProcValid = new boolean[MAXDEPTH];
	private int procDepth;

	/**
	 * Method to read a Calibre file and create Technology Edit cells.
	 */
	public static void readCalibre()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, null);
		if (fileName == null) return;

		Calibre cal = new Calibre();
		URL url = TextUtils.makeURLToFile(fileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			cal.lineReader = new LineNumberReader(is);

			// make a Calibre object and read the file
			cal.readFile();
			cal.lineReader.close();
			System.out.println(fileName + " read");
			cal.dumpLayers();
		} catch (IOException e)
		{
			System.out.println("Error reading " + fileName);
			return;
		}
	}

	private void dumpLayers()
	{
		List layerNames = new ArrayList();
		for(Iterator it = allLayers.keySet().iterator(); it.hasNext(); )
			layerNames.add(it.next());
		Collections.sort(layerNames, TextUtils.STRING_NUMBER_ORDER);
		for(Iterator it = layerNames.iterator(); it.hasNext(); )
		{
			String layerName = (String)it.next();
			Layer lay = (Layer)allLayers.get(layerName);
			if (lay.ignore) continue;
			if (lay.derived)
			{
				Layer eval = evaluateLayerExpression(lay.derivedRule);
				if (eval == null) continue;
				System.out.print(lay.name + " DERIVED:");
				for(Iterator rIt = lay.derivedRule.iterator(); rIt.hasNext(); )
				{
					String part = (String)rIt.next();
					System.out.print(" " + part);
				}
				System.out.println();
			} else
			{
				System.out.println(lay.name+" ON GDS "+lay.gdsNumber+" IS "+lay.comment);
			}
		}
	}

	/**
	 * Method to evaluate a derived layer rule.
	 * Examples are:
	 *     AA = COPY BB
	 *     AA = BB NOT CC     BB subtract CC
	 *     AA = BB AND CC     intersection
	 *     AA = BB OR CC      union
	 *     AA = BB [NOT] INTERACT CC [== 22]            returns distance between BB and CC
	 *     AA = BB [NOT] TOUCH CC [== 22]   (touch, not touch, touch edge)
	 *     AA = BB ENCLOSE CC          returns BB that has CC in it
	 *     AA = BB ANGLE == 22
	 *     AA = BB WITH EDGE CC
	 *     AA = STAMP BB BY CC
	 *     AA = HOLES BB [INNER]
	 *     AA = AREA BB == 22
	 *     AA = LENGTH BB == 22
	 *     AA = SIZE BB BY 20 [UNDEROVER] [TRUNCATE 55]
	 *     AA = SIZE BB BY 55 INSIDE OF CC STEP 11 [TRUNCATE 22]
	 *     AA = NOT RECTANGLE BB == 22 BY == 33 ORTHOGONAL ONLY
	 *     AA = RECTANGLE ENCLOSURE BB CC ABUT<90 [SINGULAR] GOOD 11 22 OPPOSITE 33 44 OPPOSITE
	 *     AA = ENCLOSE RECTANGLE BB 33 44
	 *     AA = BB [NOT] INSIDE [EDGE] CC
	 *     AA = BB [NOT] COIN INSIDE EDGE CC
	 *     AA = BB [NOT] OUTSIDE [EDGE] CC
	 *     AA = BB [NOT] COIN OUTSIDE EDGE CC
	 *     AA = EXPAND EDGE BB OUTSIDE BY 32 [EXTEND BY 12]
	 *     AA = EXT BB CC == 22 ABUT<90 OPPOSITE REGION
	 *     AA = EXT BB CC == 22 ABUT<90 SINGULAR REGION
	 *     AA = EXT BB CC == 22 OPPOSITE REGION MEASURE ALL
	 *     AA = EXT BB == 22 ABUT<90 REGION INTERSECTING ONLY
	 *     AA = EXT BB [CC] == 22 ABUT<90
	 *     AA = EXT BB == 22 CORNER REGION
	 *     AA = INT BB == 22 ABUT<90 [OPPOSITE] REGION
	 *     AA = INT [BB] == 22 ABUT<90
	 *     AA = ENC [BB] CC == 22 ABUT<90 [SINGULAR]
	 *     AA = ENC [BB] CC == 22 ABUT<90 OPPOSITE
	 *     AA = DENSITY BB CC == 22 WINDOW 22 STEP 33 BACKUP PRINT message
	 * 
	 * Where ABUT<90 can also be:  ABUT==90   ABUT==0   ABUT>0<90
	 * Where ==22 can also be:  >22   <22   >=22   <=22
	 * Parentheses group
	 * COIN mean "coincident"
	 * @param keywords the array of keywords in the expression
	 * @return unknown.
	 */
	private Layer evaluateLayerExpression(List keywords)
	{
		// replace layer names with identifiers
		for(Iterator it = keywords.iterator(); it.hasNext(); )
		{
			String key = (String)it.next();
			
		}
		return null;
	}

	private void readFile()
		throws IOException
	{
		allRules = new HashMap();
		allVariables = new HashMap();
		allLayers = new HashMap();
		allDefines = new HashSet();
		procDepth = -1;
		for(;;)
		{
			curLine = nextLine();
			if (curLine == null) break;

			String [] keywords = curLine.split("\\s+");
			if (keywords.length == 0) continue;
			if (keywords[0].equalsIgnoreCase("#define")) { handleSharp(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("#ifdef")) { handleSharp(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("#ifndef")) { handleSharp(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("#else")) { handleSharp(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("#endif")) { handleSharp(keywords);   continue; }

			// ignore things that are preprocessor-ed out
			if (procDepth >= 0 && !preProcValid[procDepth]) continue;

			if (keywords[0].equalsIgnoreCase("precision")) { handlePrecision(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("connect")) { handleConnect(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("disconnect")) { handleDisconnect(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("resolution")) { handleResolution(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("drc")) { handleDRC(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("flag")) { handleFlag(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("layer")) { handleLayer(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("layout")) { handleLayout(keywords);   continue; }
			if (keywords[0].equalsIgnoreCase("variable")) { handleVariable(keywords);   continue; }
			handleUnknown(keywords);
		}
	}

	private void handleSharp(String [] keywords)
	{
		if (keywords[0].equalsIgnoreCase("#define"))
		{
			if (keywords.length < 2)
			{
				System.out.println("Line " + lineReader.getLineNumber() + ", #DEFINE missing keyword: " + curLine);
				return;
			}
			allDefines.add(keywords[1]);
			return;
		}

		if (keywords[0].equalsIgnoreCase("#ifdef"))
		{
			if (keywords.length < 2)
			{
				System.out.println("Line " + lineReader.getLineNumber() + ", #IFDEF missing keyword: " + curLine);
				return;
			}
			procDepth++;
			preProcValid[procDepth] = allDefines.contains(keywords[1]);
			return;
		}

		if (keywords[0].equalsIgnoreCase("#ifndef"))
		{
			if (keywords.length < 2)
			{
				System.out.println("Line " + lineReader.getLineNumber() + ", #IFNDEF missing keyword: " + curLine);
				return;
			}
			procDepth++;
			preProcValid[procDepth] = !allDefines.contains(keywords[1]);
			return;
		}

		if (keywords[0].equalsIgnoreCase("#else"))
		{
			if (procDepth < 0)
			{
				System.out.println("Line " + lineReader.getLineNumber() + ", #ELSE unmatched");
				return;
			}
			preProcValid[procDepth] = !preProcValid[procDepth];
			return;
		}

		if (keywords[0].equalsIgnoreCase("#endif"))
		{
			if (procDepth < 0)
			{
				System.out.println("Line " + lineReader.getLineNumber() + ", #ENDIF unmatched");
				return;
			}
			procDepth--;
			return;
		}
	}

	private void handleResolution(String [] keywords)
	{
		if (keywords.length < 2)
		{
			System.out.println("Line " + lineReader.getLineNumber() + ", RESOLUTION line missing value: " + curLine);
			return;
		}
		resolution = TextUtils.atof(keywords[1]);
	}

	private void handleConnect(String [] keywords)
	{
	}

	private void handleDisconnect(String [] keywords)
	{
	}

	private void handleDRC(String [] keywords)
	{
	}

	private void handleFlag(String [] keywords)
	{
	}

	private void handleLayer(String [] keywords)
	{
		if (keywords.length < 3)
		{
			System.out.println("Line " + lineReader.getLineNumber() + ", LAYER line too short: " + curLine);
			return;
		}

		// check for special layer keywords
		if (keywords[1].equalsIgnoreCase("map"))
		{
			return;
		}
		if (keywords[1].equalsIgnoreCase("resolution"))
		{
			return;
		}

		// create a new layer
		Layer lay = new Layer();
		lay.derived = false;
		lay.name = keywords[1];
		lay.gdsNumber = TextUtils.atoi(keywords[2]);
		int comment = curLine.indexOf("//");
		if (comment >= 0)
			lay.comment = curLine.substring(comment+2).trim();

		// test code: make metal and via layers valid, all else invalid
		lay.ignore = true;
		if (lay.name.startsWith("VIA")) lay.ignore = false;
		if (lay.name.startsWith("M") && lay.name.endsWith("i") && lay.name.length() == 3) lay.ignore = false;
	
		if (allLayers.get(lay.name) != null)
		{
			System.out.println("Line " + lineReader.getLineNumber() + ", duplicate layer: " + curLine);
			return;
		}
		allLayers.put(lay.name, lay);
	}

	private void handleLayout(String [] keywords)
	{
	}

	private void handleVariable(String [] keywords)
	{
		if (keywords.length < 3)
		{
			System.out.println("Line " + lineReader.getLineNumber() + ", VARIABLE line too short: " + curLine);
			return;
		}
		String variable = keywords[1];
		String value = keywords[2];
		allVariables.put(variable, value);
	}

	private void handlePrecision(String [] keywords)
	{
		if (keywords.length < 2)
		{
			System.out.println("Line " + lineReader.getLineNumber() + ", PRECISION line missing value: " + curLine);
			return;
		}
		precision = TextUtils.atof(keywords[1]);
	}

	private void handleUnknown(String [] keywords)
		throws IOException
	{
		if (keywords.length > 1)
		{
			if (keywords[1].equals("="))
			{
				// a derived layer
				Layer lay = new Layer();
				lay.derived = true;
				lay.name = keywords[0];
				lay.comment = null;
				lay.ignore = false;
				lay.derivedRule = new ArrayList();
				for(int i=2; i<keywords.length; i++)
					lay.derivedRule.add(keywords[i]);
				if (allLayers.get(lay.name) != null)
				{
					System.out.println("Line " + lineReader.getLineNumber() + ", duplicate derived layer: " + curLine);
					return;
				}
				allLayers.put(lay.name, lay);
				return;
			}
			if (keywords[1].equals("{"))
			{
				// a rule
				Rule r = new Rule();
				r.name = keywords[0];
				r.comment = "";
				r.lines = new ArrayList();
				for(;;)
				{
					String buf = nextLine();
					if (buf.equals("}")) break;
					r.lines.add(buf);
				}
				allRules.put(r.name, r);
				return;
			}
		}
		System.out.println("Line " + lineReader.getLineNumber() + ", unknown line: " + curLine);
	}

	private String nextLine()
		throws IOException
	{
		for(;;)
		{
			String buf = lineReader.readLine();
			if (buf == null) return null;
			String b = buf.trim();
			if (b.length() == 0) continue;
			if (b.startsWith("//") || b.startsWith("[")) continue;
			return b;
		}
	}
}
