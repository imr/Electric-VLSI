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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.generator.layout.LayoutLib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/** Representation of the NCC annotations that a user may place on a Cell */

public class NccCellAnnotations {
	/** key of Variable holding NCC Cell annotations. */	public static final Variable.Key NCC_ANNOTATION_KEY = Variable.newKey("ATTR_NCC");

	public static class NamePattern {
		private final boolean isRegExp;
		private final String pattern;
		NamePattern(boolean isRegEx, String pat) {
			isRegExp=isRegEx; pattern=pat;
		}
		/** String match or regular expression match */
		public boolean matches(String name) {
			return isRegExp ? name.matches(pattern) : name.equals(pattern);
		}
		public boolean stringEquals(String name) {return name.equals(pattern);}
		/** @return If NamePattern is a name then return the name. If 
		 * NamePattern is a regular expression then return null. */
		public String getName() {return isRegExp ? null : pattern;}
	}
	/** I need a special Lexer for name patterns because spaces are
	 * significant inside regular expressions. For example the 
	 * pattern: "/foo bar/" contains an embedded space. */
	private class NamePatternLexer {
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
					prErr("Regular Expression has no trailing '/': "+
						  s.substring(startTok-1)+".");
					endTok = s.length();
				} else {
					pos++; // skip the trailing '/'
				}
				String pat = s.substring(startTok, endTok);
				return new NamePattern(true, pat);
			} else {
				endTok = findWhite();
				if (endTok==-1) endTok = s.length();
				return new NamePattern(false, s.substring(startTok, endTok));
			}
		}
		/** @return everything not parsed by nextPattern() */
		public String restOfLine() {return s.substring(pos);}
	}
	/** used for error messages */
	private String cellThatOwnsMe;
	/** unprocessed annotation text */
	private List<String> annotText = new ArrayList<String>();
	/** NamePatterns matching Exports connected by parent cell */
	private List<List<NamePattern>> exportsConnByParent = new ArrayList<List<NamePattern>>();
	/** reason given by user for skipping NCC of this Cell */
	private String skipReason;
	/** reason given by user for treating this Cell as a subcircuit 
	 * during hierarchical NCC*/
	private String notSubcircuitReason;
	/** the CellGroup that this cell should join */
	private Cell.CellGroup groupToJoin;
	/** NamePatterns matching instance names to flatten */
	private List<NamePattern> flattenInstances = new ArrayList<NamePattern>();
	/** NamePatterns matching Export names that need renaming */
	private List<NamePattern> exportsToRename = new ArrayList<NamePattern>();
	/** Reason why we should treat this Cell as a black box */
	private String blackBoxReason;
	/** Cell annotation describing type of contained MOS */
	private String transistorType;
	/** Cell annotation describing type of contained resistor */
	private String resistorType;
	/** List of names of Wires to force matches between schematic and layout*/
	private List<String> forceWireMatches = new ArrayList<String>();
	/** List of names of Wires to force matches between schematic and layout*/
	private List<String> forcePartMatches = new ArrayList<String>();
	
	private void processExportsConnAnnot(NamePatternLexer lex) {
		List<NamePattern> connected = new ArrayList<NamePattern>();
		for (NamePattern np=lex.nextPattern(); np!=null; np=lex.nextPattern()) {
			connected.add(np);
		}
		if (connected.size()>0) exportsConnByParent.add(connected); 
	}

	private void processSkipAnnotation(NamePatternLexer lex) {
		skipReason = lex.restOfLine();
	}
	
	private void processNotSubcircuitAnnotation(NamePatternLexer lex) {
		notSubcircuitReason = lex.restOfLine();
	}

	private void prErr(String s) {
		String currAnnot = annotText.get(annotText.size()-1);
		System.out.println("  "+s+"  cell= "+cellThatOwnsMe+" annotation= "+currAnnot);
	}
	private void processJoinGroupAnnotation(String note) {
		StringTokenizer lex = new StringTokenizer(note);
		lex.nextToken(); // skip keyword
		if (!lex.hasMoreTokens()) {
			prErr("joinGroup lacks Library:Cell argument.");
			return;
		}
		String libCell = lex.nextToken();
		int colon = libCell.indexOf(':');
		if (colon==-1) {
			prErr("Group specification must be of form Library:Cell{view}.");
			return;
		}
		String libName = libCell.substring(0, colon);
		String cellName = libCell.substring(colon+1);
		Library lib = Library.findLibrary(libName);
		if (lib==null) {
			prErr("Can't find library: "+libName+".");
			return;
		}
		Cell cell = lib.findNodeProto(cellName);
		if (cell==null) {
			prErr("Can't find Cell "+cellName+".");
			return;
		}
		groupToJoin = cell.getCellGroup();
		LayoutLib.error(groupToJoin==null, "null cell group?");
	}

	private void processFlattenInstancesAnnotation(NamePatternLexer lex) {
		for (NamePattern np=lex.nextPattern(); np!=null; np=lex.nextPattern()) {
			flattenInstances.add(np);
		}
	}

	private void processExportsToRenameAnnotation(NamePatternLexer lex) {
		for (NamePattern np=lex.nextPattern(); np!=null; np=lex.nextPattern()) {
			exportsToRename.add(np);
		}
	}
	private void processBlackBox(NamePatternLexer lex) {
		blackBoxReason = lex.restOfLine();
	}
	private void processTransistorType(NamePatternLexer lex) {
		NamePattern type = lex.nextPattern();
		if (type==null) {
			prErr("Bad transistorType annotation: missing type");
			return;
		}
		NamePattern type2 = lex.nextPattern();
		if (type2!=null) {
			prErr("Bad transistorType annotation: only one type allowed");
			return;
		}
		if (transistorType!=null) {
			prErr("only one transistorType annotation allowed per Cell");
			return;
		}
		transistorType = type.getName();
		if (transistorType==null) {
			prErr("Transistor type may not be a regular expression");
			return;
		}
	}
	private void processResistorType(NamePatternLexer lex) {
		NamePattern type = lex.nextPattern();
		if (type==null) {
			prErr("Bad resistorType annotation: missing type");
			return;
		}
		NamePattern type2 = lex.nextPattern();
		if (type2!=null) {
			prErr("Bad resistorType annotation: only one type allowed");
			return;
		}
		if (resistorType!=null) {
			prErr("only one resistorType annotation allowed per Cell");
			return;
		}
		resistorType = type.getName();
		if (resistorType==null) {
			prErr("resistor type may not be a regular expression");
			return;
		}
	}
	private void processForceWireMatch(NamePatternLexer lex) {
		NamePattern wireNamePat = lex.nextPattern();
		if (wireNamePat==null) {
			prErr("Bad forceWireMatch annotation: missing Wire name");
			return;
		}
		String wireName = wireNamePat.getName();
		if (wireName==null) {
			prErr("Bad forceWireMatch annotation: Wire name may not be a regular expression");
			return;
		}
		forceWireMatches.add(wireName);
		NamePattern namePat2 = lex.nextPattern();
		if (namePat2!=null) {
			prErr("Bad forceWireMatch annotation: only one Wire name allowed");
		}
	}

	private void processForcePartMatch(NamePatternLexer lex) {
		NamePattern partNamePat = lex.nextPattern();
		if (partNamePat==null) {
			prErr("Bad forcePartMatch annotation: missing Part name");
			return;
		}
		String partName = partNamePat.getName();
		if (partName==null) {
			prErr("Bad forcePartMatch annotation: Part name may not be a regular expression");
			return;
		}
		forcePartMatches.add(partName);
		NamePattern namePat2 = lex.nextPattern();
		if (namePat2!=null) {
			prErr("Bad forcePartMatch annotation: only one Part name allowed");
		}
	}

	private void doAnnotation(String note) {
		annotText.add(note); // for prErr()
		NamePatternLexer lex = new NamePatternLexer(note);
		NamePattern key = lex.nextPattern();
		if (key==null) {
			// skip blank lines
		} else if (key.stringEquals("exportsConnectedByParent")) {
			processExportsConnAnnot(lex);
		} else if (key.stringEquals("skipNCC")) {
			processSkipAnnotation(lex);
		} else if (key.stringEquals("notSubcircuit")) {
			processNotSubcircuitAnnotation(lex);
		} else if (key.stringEquals("joinGroup")) {
			processJoinGroupAnnotation(note);
		} else if (key.stringEquals("flattenInstances")) {
			processFlattenInstancesAnnotation(lex);
		} else if (key.stringEquals("exportsToRename")) {
			processExportsToRenameAnnotation(lex);
		} else if (key.stringEquals("blackBox")) {
			processBlackBox(lex);
		} else if (key.stringEquals("transistorType")) {
			processTransistorType(lex);
		} else if (key.stringEquals("resistorType")) {
			processResistorType(lex);
		} else if (key.stringEquals("forceWireMatch")) {
			processForceWireMatch(lex);
		} else if (key.stringEquals("forcePartMatch")) {
			processForcePartMatch(lex);
		} else {
			prErr("Unrecognized NCC annotation.");
		}
	}

	private NccCellAnnotations(Cell cell, Object annotation) {
		cellThatOwnsMe = NccUtils.fullName(cell); // for prErr()
		if (annotation instanceof String) {
			doAnnotation((String) annotation);
		} else if (annotation instanceof String[]) {
			String[] ss = (String[]) annotation;
			for (int i=0; i<ss.length; i++)  doAnnotation(ss[i]);
		} else {
			prErr(" ignoring bad NCC annotation: ");
		}
	}

	/**
	 * Class to create a Cell NCC annotation object in a new Job.
	 */
    private static class MakeCellAnnotation extends Job {
    	static final long serialVersionUID = 0;
    	
		private transient EditWindow_ wnd;
        private Cell cell;
        private String newAnnotation;

		private MakeCellAnnotation(EditWindow_ wnd, Cell cell, String annotation)
        {
            super("Make Cell NCC Annotation", NetworkTool.getNetworkTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.wnd = wnd;
            this.cell = cell;
            newAnnotation = annotation;
            startJob();
        }

        public boolean doIt() throws JobException {
        	
			Variable nccVar = cell.getVar(NCC_ANNOTATION_KEY);
			if (nccVar == null) {
				String [] initial = new String[1];
				initial[0] = newAnnotation;
				TextDescriptor td = TextDescriptor.getCellTextDescriptor().withInterior(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
				nccVar = cell.newVar(NCC_ANNOTATION_KEY, initial, td);
				if (nccVar == null) return true;
			} else {
				Object oldObj = nccVar.getObject();
				if (oldObj instanceof String) {
					/* Groan! Menu command always creates NCC attributes as arrays of strings.
					 * However, if user edits a single line NCC attribute then dialog box
					 * converts it back into a String.  Be prepared to convert it back into an array*/
					oldObj = new String[] {(String)oldObj};
				}
				LayoutLib.error(!(oldObj instanceof String[]), "NCC annotation not String[]");
				String[] oldVal = (String[]) oldObj;
				TextDescriptor td = nccVar.getTextDescriptor();

				int newLen = oldVal.length+1;
				String[] newVal = new String[newLen];
				for (int i=0; i<newLen-1; i++) newVal[i]=oldVal[i];
				newVal[newLen-1] = newAnnotation;
				nccVar = cell.newVar(NCC_ANNOTATION_KEY, newVal, td);
			}
			return true;
        }
        public void terminateOK() {
        	wnd.clearHighlighting();
			wnd.addHighlightText(cell, cell, NCC_ANNOTATION_KEY);
			wnd.finishedHighlighting();
        }
    }

	// ---------------------- public methods -----------------------------

	/**
	 * Method to create NCC annotations in the current Cell.
	 * Called from the menu commands.
	 */
	public static void makeNCCAnnotation(String newAnnotation)
	{
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.needCurrentEditWindow_();
		if (wnd == null) return;
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		new MakeCellAnnotation(wnd, cell, newAnnotation);
	}

	/**
	 * Method to get the NCC annotations on a Cell.
	 * @param cell the Cell to query.
	 * @return the NccCellAnnotations for the Cell.
	 * Returns null if the Cell has no NCC annotations
	 */
	public static NccCellAnnotations getAnnotations(Cell cell) {
		Variable nccVar = cell.getVar(NCC_ANNOTATION_KEY);
		if (nccVar==null) return null;
		Object annotation = nccVar.getObject();
		return new NccCellAnnotations(cell, annotation);
	}

	/** @return a String which is the reason given by the user for not 
	 * NCCing the cell or null if there is no skipNCC annotation on 
	 * the cell. */
	public String getSkipReason() {return skipReason;}
	/** @return the reason given by the user for not treating this Cell
	 * as a subcircuit during hierarchical NCC. Return null if there is
	 * no notSubcircuitReason annotation on the Cell */
	public String getNotSubcircuitReason() {return notSubcircuitReason;}
	/** @return an Iterator over Lists of NamePatterns. Each List specifies 
	 * the names (or regular expressions that match the names) of Exports 
	 * that the user expects to be connected by the Cell's parent. */  
	public Iterator<List<NamePattern>> getExportsConnected() {return exportsConnByParent.iterator();}
	
	public Iterator<String> getAnnotationText() {return annotText.iterator();}
	public Cell.CellGroup getGroupToJoin() {return groupToJoin;}
	public boolean flattenInstance(String instName) {
		for (NamePattern pattern : flattenInstances) {
			if (pattern.matches(instName)) return true;
		}
		return false;
	}
	public boolean renameExport(String exportName) {
		for (NamePattern pattern : exportsToRename) {
			if (pattern.matches(exportName)) return true;
		}
		return false;
	}
	/** @return the reason given by the user for block boxing this Cell.
	 * return null if there is no blackBox annotation. */
	public String getBlackBoxReason() {return blackBoxReason;}
	/** @return the transistor type if Cell has a transitorType annotation.
	 * Otherwise return null. */
	public String getTransistorType() {return transistorType;}
	/** @return the resistor type if Cell has a resistorType annotation.
	 * Otherwise return null. */
	public String getResistorType() {return resistorType;}
	/** @return the names of Wires for which we should force matches */
	public List<String> getForceWireMatches() {return forceWireMatches;}
	/** @return the names of Wires for which we should force matches */
	public List<String> getForcePartMatches() {return forcePartMatches;}
}
