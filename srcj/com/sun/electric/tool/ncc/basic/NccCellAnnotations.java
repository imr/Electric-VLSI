/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccCellAnnotations.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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
package com.sun.electric.tool.ncc.basic;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Representation of the NCC annotations that a user may place on a Cell */

public class NccCellAnnotations {
	public static class NamePattern {
		private final boolean isRegExp;
		private final String pattern;
		NamePattern(boolean isRegEx, String pat) {
			isRegExp=isRegEx; pattern=pat;
		}
		public boolean matches(String name) {
			return isRegExp ? name.matches(pattern) : name.equals(pattern);
		}
	}
	/** I need a special Lexer for name patterns because spaces are
	 * significant inside regular expressions. For example the 
	 * pattern: "/foo bar/" contains an embedded space. */
	private static class NamePatternLexer {
		private final String s;
		private int pos=0;

		private int findWhite() {
			while (true) {
				if (pos>=s.length()) return -1;
				if (Character.isWhitespace(s.charAt(pos))) return pos;
				pos++;
			} 
		}
		/** @return index of first white char or -1 if no white chars 
		 * between here and end of line */
		private int findNonWhite() {
			while (true) {
				if (pos>=s.length()) return -1;
				if (!Character.isWhitespace(s.charAt(pos))) return pos;
				pos++;
			} 
		}
		private int findSlash() {
			while (true) {
				if (pos>=s.length()) return -1;
				if (s.charAt(pos)=='/') return pos;
				pos++;
			}
		}
		public NamePatternLexer(String annot) {s = annot;}

		/** @return null when no more patterns left */
		public NamePattern nextPattern() {
			int startTok = findNonWhite();
			if (startTok==-1) return null;
			int endTok;
			if (s.charAt(startTok)=='/') {
				pos++;	// skip the leading '/'
				startTok = pos;
				endTok = findSlash();
				if (endTok==-1) {
					System.out.println("Regular Expression has no trailing '/'"+
									   s.substring(startTok-1));
					endTok = s.length();
				} else {
					pos++; // skip the trailing '/'
				}
				String pat = s.substring(startTok, endTok);
				//System.out.println("       regExp: "+pat);
				return new NamePattern(true, pat);
			} else {
				endTok = findWhite();
				if (endTok==-1) endTok = s.length();
				//System.out.println("        name: "+s.substring(startTok, endTok));
				return new NamePattern(false, s.substring(startTok, endTok));
			}
		}
	}
	/** unprocessed annotation text */
	private List annotText = new ArrayList();
	/** NamePatterns matching Exports connected by parent cell */
	private List exportsConnByParent = new ArrayList();
	/** reason given by user for skipping NCC of this Cell */
	private String skipReason;
	/** the CellGroup that this cell should join */
	private Cell.CellGroup groupToJoin;
	/** NamePatterns matching instance names to flatten */
	private List flattenInstances = new ArrayList();
	
	private void processExportsConnAnnot(String note) {
		List connected = new ArrayList();
		NamePatternLexer lex = new NamePatternLexer(note);
		lex.nextPattern();	// skip the keyword
		for (NamePattern np=lex.nextPattern(); np!=null; np=lex.nextPattern()) {
			connected.add(np);
		}
		if (connected.size()>0) exportsConnByParent.add(connected); 
	}

	private void processSkipAnnotation(String note) {
		skipReason = "";
		int sp = note.indexOf(" ");
		if (sp!=-1) skipReason = note.substring(sp);
	}
	
	private void processJoinGroupAnnotation(String note) {
		StringTokenizer lex = new StringTokenizer(note);
		lex.nextToken(); // skip keyword
		if (!lex.hasMoreTokens()) {
			System.out.println("Missing Library:Cell argument "+note);
			return;
		}
		String libCell = lex.nextToken();
		int colon = libCell.indexOf(':');
		if (colon==-1) {
			System.out.println(
				"group specification must be of form Library:Cell "+note);
			return;
		}
		String libName = libCell.substring(0, colon);
		String cellName = libCell.substring(colon+1);
		Library lib = Library.findLibrary(libName);
		if (lib==null) {
			System.out.println("Can't find library "+libName+
				" from annotation "+note);
			return;
		}
		Cell cell = lib.findNodeProto(cellName);
		if (cell==null) {
			System.out.println("Can't find Cell "+cellName+
				" from annotation "+note);
			return;
		}
		groupToJoin = cell.getCellGroup();
		LayoutLib.error(groupToJoin==null, "null cell group?");
	}

	private void processFlattenInstancesAnnotation(String note) {
		NamePatternLexer lex = new NamePatternLexer(note);
		lex.nextPattern();	// skip the keyword
		for (NamePattern np=lex.nextPattern(); np!=null; np=lex.nextPattern()) {
			flattenInstances.add(np);
		}
	}

	private void doAnnotation(String note) {
		annotText.add(note);
		if (note.startsWith("exportsConnectedByParent")) {
			processExportsConnAnnot(note);
		} else if (note.startsWith("skipNCC")) {
			processSkipAnnotation(note);
		} else if (note.startsWith("joinGroup")) {
			processJoinGroupAnnotation(note);
		} else if (note.startsWith("flattenInstances")) {
			processFlattenInstancesAnnotation(note);
		} else {
			System.out.println("Unrecognized NCC annotation: "+note);
		}
	}

	private NccCellAnnotations(Cell cell, Object annotation) {
		if (annotation instanceof String) {
			doAnnotation((String) annotation);
		} else if (annotation instanceof String[]) {
			String[] ss = (String[]) annotation;
			for (int i=0; i<ss.length; i++)  doAnnotation(ss[i]);
		} else {
			System.out.println("ignoring bad NCC annotation: "+annotation+
							   " on Cell: "+cell.getName());
		}
	}
	
	// ---------------------- public methods -----------------------------

	/** @return null if Cell has no NCC annotations */
	public static NccCellAnnotations getAnnotations(Cell cell) {
		Variable nccVar = cell.getVar("ATTR_NCC");
		if (nccVar==null) return null;
		Object annotation = nccVar.getObject();
		return new NccCellAnnotations(cell, annotation);
	}
	/** @return a String which is the reason given by the user for not 
	 * NCCing the cell or null if there is no skipNCC annotation on 
	 * the cell. */
	public String getSkipReason() {return skipReason;}
	
	/** @return an Iterator over Lists of NamePatterns. Each List specifies 
	 * the names (or regular expressions that match the names) of Exports 
	 * that the user expects to be connected by the Cell's parent. */  
	public Iterator getExportsConnected() {return exportsConnByParent.iterator();}
	
	public Iterator getAnnotationText() {return annotText.iterator();}
	public Cell.CellGroup getGroupToJoin() {return groupToJoin;}
	public boolean flattenInstance(String instName) {
		for (Iterator it=flattenInstances.iterator(); it.hasNext();) {
			NamePattern pattern = (NamePattern) it.next();
			if (pattern.matches(instName)) return true;
		}
		return false;
	}
}
