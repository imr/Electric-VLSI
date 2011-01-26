/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CompileVerilogStruct.java
 * Compile Structural Verilog to a netlist
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.user;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.math.DBMath;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the structural Verilog compiler.
 */
public class CompileVerilogStruct
{
	private List<VModule> allModules;
	private int errorCount;
	private boolean hasErrors;
	private VModule curModule;
	private List<TokenList> tList;
	private int tokenIndex;

	/********** Scanner Token Types ******************************************/

	public static class TokenType
	{
		private String name, str;

		private TokenType(String name, String str)
		{
			this.name = name;
			this.str = str;
		}

		public String getName() { return name; }

		public String getChar() { return str; }

		public static final TokenType LEFTPAREN = new TokenType("Left Parenthesis", "(");
		public static final TokenType RIGHTPAREN = new TokenType("Right Parenthesis", ")");
		public static final TokenType LEFTBRACKET = new TokenType("Left Bracket", "[");
		public static final TokenType RIGHTBRACKET = new TokenType("Right Bracket", "]");
		public static final TokenType LEFTBRACE = new TokenType("Left Brace", "{");
		public static final TokenType RIGHTBRACE = new TokenType("Right Brace", "}");
		public static final TokenType SLASH = new TokenType("Forward Slash", "/");
		public static final TokenType COMMA = new TokenType("Comma", ",");
		public static final TokenType MINUS = new TokenType("Minus", "-");
		public static final TokenType PERIOD = new TokenType("Period", ".");
		public static final TokenType COLON = new TokenType("Colon", ":");
		public static final TokenType SEMICOLON = new TokenType("Semicolon", ";");
		public static final TokenType DOUBLEDOT = new TokenType("DotDot", "..");
		public static final TokenType VARASSIGN = new TokenType("Assign", "=>");
		public static final TokenType UNKNOWN = new TokenType("Unknown", "");
		public static final TokenType IDENTIFIER = new TokenType("Identifier", "");
		public static final TokenType KEYWORD = new TokenType("Keyword", "");
		public static final TokenType DECIMAL = new TokenType("Decimal Number", "");
		public static final TokenType CHAR = new TokenType("Character", "");
		public static final TokenType STRING = new TokenType("String", "");
	}

	/********** Scanner Tokens *****************************************/

	public class TokenList
	{
		/** token number */								TokenType	type;
		/** NULL if delimiter,
		 * pointer to global name space if identifier,
		 * pointer to keyword table if keyword,
		 * pointer to string if decimal literal,
		 * pointer to string if based literal,
		 * value of character if character literal,
		 * pointer to string if string literal,
		 * pointer to string if bit string literal */	Object	pointer;
		 /** TRUE if space before next token */			boolean	space;
		 /** line number token occurred */				int	lineNum;

		private TokenList(TokenType type, Object pointer, int lineNum, boolean space)
		{
			this.type = type;
			this.pointer = pointer;
			this.lineNum = lineNum;
			this.space = true;
			tList.add(this);
		}

		public int makeErrorLine(StringBuffer buffer)
		{
			int index = tList.indexOf(this);
			int lineNumber = this.lineNum;

			// back up to start of line
			while (index > 0 && tList.get(index-1).lineNum == lineNumber) index--;

			// form line in buffer
			int pointer = 0;
			for(int i=index; i<tList.size(); i++)
			{
				TokenList tok = tList.get(i);
				if (tok.lineNum != lineNumber) break;
				if (tok == this) pointer = buffer.length();
				buffer.append(tok.toString());
				if (tok.space) buffer.append(" ");
			}
			return pointer;
		}

		public String toString()
		{
			if (type == TokenType.STRING) return "\"" + pointer + "\" ";
			if (type == TokenType.KEYWORD) return ((VKeyword)pointer).name;
			if (type == TokenType.DECIMAL) return (String)pointer;
			if (type == TokenType.CHAR) return ((Character)pointer).charValue() + "";
			if (type == TokenType.IDENTIFIER)
			{
				if (pointer == null) return "NULL";
				return pointer.toString();
			}
			return type.getChar();
		}
	}

	private void resetTokenListPointer() { tokenIndex = 0; }

	private TokenList getNextToken()
	{
		if (tokenIndex >= tList.size()) return null;
		TokenList token = tList.get(tokenIndex++);
		return token;
	}

	private TokenList peekNextToken()
	{
		if (tokenIndex >= tList.size()) return null;
		return tList.get(tokenIndex);
	}

	private TokenType getTokenType(TokenList token)
	{
		if (token == null) return TokenType.UNKNOWN;
		return token.type;
	}

	private TokenList needNextToken(TokenType type)
	{
		TokenList token = getNextToken();
		if (token == null)
		{
			reportErrorMsg(null, "End of file encountered");
			return null;
		}
		if (token.type != type)
		{
			reportErrorMsg(token, "Expecting a " + type.getName());
			parseToSemicolon();
			return null;
		}
		return token;
	}

	/********** Keywords ******************************************/

	public static class VKeyword
	{
		/** string defining keyword */	String	name;
		private static List<VKeyword> theKeywords = new ArrayList<VKeyword>();

		VKeyword(String name)
		{
			this.name = name;
			theKeywords.add(this);
		}

		public static VKeyword findKeyword(String tString)
		{
			for(VKeyword vk : theKeywords)
			{
				if (vk.name.equals(tString)) return vk;
			}
			return null;
		}

		public static final VKeyword ASSIGN    = new VKeyword("assign");
		public static final VKeyword ENDMODULE = new VKeyword("endmodule");
		public static final VKeyword INOUT     = new VKeyword("inout");
		public static final VKeyword INPUT     = new VKeyword("input");
		public static final VKeyword MODULE    = new VKeyword("module");
		public static final VKeyword OUTPUT    = new VKeyword("output");
		public static final VKeyword SUPPLY    = new VKeyword("supply");
		public static final VKeyword SUPPLY0   = new VKeyword("supply0");
		public static final VKeyword WIRE      = new VKeyword("wire");
	};

	/********** Modules *************************************************/

	public class VModule
	{
		/** name of entity */			String				name;
		/** true if cell in Verilog */	boolean				defined;
		/** cell of this module */		Cell				cell;
		/** list of ports */			List<FormalPort>	ports;
		/** list of internal wires */	List<String>		wires;
		/** list of instances */		List<Instance>		instances;
		/** networks in the module */	Map<String,List<LocalPort>> allNetworks;

		VModule(String name, boolean defined)
		{
			this.name = name;
			this.defined = defined;
			cell = null;
			if (!defined)
			{
				// find the cell
				for(Library lib : Library.getVisibleLibraries())
				{
					for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
					{
						Cell libCell = it.next();
						if (libCell.getName().equals(name))
						{
							for(Iterator<Cell> gIt = libCell.getCellGroup().getCells(); gIt.hasNext(); )
							{
								Cell rightOne = gIt.next();
								if (rightOne.getView() == View.LAYOUT)
								{
									cell = rightOne;
									break;
								}
							}
							if (cell == null)
							{
								cell = libCell;
								break;
							}
						}
					}
					if (cell != null) break;
				}
			}

			ports = new ArrayList<FormalPort>();
			wires = new ArrayList<String>();
			instances = new ArrayList<Instance>();
			allNetworks = new HashMap<String,List<LocalPort>>();
			allModules.add(this);
		}
	};

	private VModule findModule(String name)
	{
		for(VModule mod : allModules)
			if (mod.name.equals(name)) return mod;
		return null;
	}

	/********** Ports on Modules **********************************/

	private static final int MODE_UNKNOWN		= 0;
	private static final int MODE_IN			= 1;
	private static final int MODE_OUT			= 2;
	private static final int MODE_INOUT			= 3;

	public static class FormalPort
	{
		/** name of port */				String		name;
		/** mode of port */				int			mode;
		/** range of port */			int			firstIndex, secondIndex;

		public FormalPort(String name)
		{
			this.name = name;
			mode = MODE_UNKNOWN;
			firstIndex = secondIndex = -1;
		}
	};

	/********** Instances **********************************************/

	public static class Instance
	{
		VModule module;
		String instanceName;
		private Map<LocalPort,String> ports;

		public Instance(VModule module, String instanceName)
		{
			this.module = module;
			this.instanceName = instanceName;
			ports = new HashMap<LocalPort,String>();
		}

		public void addPort(LocalPort lp, String signalName)
		{
			ports.put(lp, signalName);
		}
	}

	/********** Ports on Instances **********************************/

	public static class LocalPort
	{
		/** Instance */					Instance	in;
		/** name of port */				String		portName;

		public LocalPort(Instance in, String portName)
		{
			this.in = in;
			this.portName = portName;
		}
	};

	/**
	 * The constructor compiles the Verilog and produces a netlist.
	 */
	public CompileVerilogStruct(Cell verilogCell)
	{
		String [] strings = verilogCell.getTextViewContents();
		if (strings == null)
		{
			System.out.println("Cell " + verilogCell.describe(true) + " has no text in it");
			return;
		}

		Job.getUserInterface().startProgressDialog("Compiling Verilog", null);
		Job.getUserInterface().setProgressNote("Scanning...");
		allModules = new ArrayList<VModule>();
		tList = new ArrayList<TokenList>();
		errorCount = 0;
		hasErrors = false;
		doScanner(strings);
		Job.getUserInterface().setProgressNote("Parsing...");
		Job.getUserInterface().setProgressValue(0);
		doParser();
		Job.getUserInterface().stopProgressDialog();

		// make network list
		for(VModule module : allModules)
		{
			for(Instance in : module.instances)
			{
				for(LocalPort lp : in.ports.keySet())
				{
					String signalName = in.ports.get(lp);
					List<LocalPort> portsOnNet = module.allNetworks.get(signalName);
					if (portsOnNet ==  null) module.allNetworks.put(signalName, portsOnNet = new ArrayList<LocalPort>());
					portsOnNet.add(lp);
				}
			}
		}
//		dumpData();
	}
	
//	private void dumpData()
//	{
//		// write what was found
//		for(VModule vm : allModules)
//		{
//			System.out.println();
//			System.out.print("++++ MODULE "+vm.name+"(");
//			boolean first = true;
//			for(FormalPort fp : vm.ports)
//			{
//				if (!first) System.out.print(", ");
//				first = false;
//				System.out.print(fp.name);
//				if (fp.firstIndex != -1 && fp.secondIndex != -1)
//					System.out.print("[" + fp.firstIndex + ":" + fp.secondIndex + "]");
//				switch (fp.mode)
//				{
//					case MODE_IN:    System.out.print(" input");   break;
//					case MODE_OUT:   System.out.print(" output");  break;
//					case MODE_INOUT: System.out.print(" inout");   break;
//				}
//			}
//			System.out.println(")");
//			if (!vm.defined)
//			{
//				if (vm.cell == null) System.out.println("     CELL NOT FOUND"); else
//					System.out.println("     CELL IS "+vm.cell.describe(false));
//			}
//			for(Instance in : vm.instances)
//			{
//				System.out.print("     INSTANCE "+in.instanceName+" OF CELL "+in.module.name+"(");
//				first = true;
//				for(LocalPort lp : in.ports.keySet())
//				{
//					if (!first) System.out.print(", ");
//					first = false;
//					String netName = in.ports.get(lp);
//					System.out.print(lp.portName+"="+netName);
//				}
//				System.out.println(")");
//			}
//			for(String netName : vm.allNetworks.keySet())
//			{
//				System.out.print("     NETWORK " + netName + " ON");
//				List<LocalPort> ports = vm.allNetworks.get(netName);
//				for(LocalPort lp : ports)
//					System.out.print(" " + lp.in.instanceName+":"+lp.portName);
//				System.out.println();
//			}
//		}
//	}

	/**
	 * Method to report whether the Verilog compile was successful.
	 * @return true if there were errors.
	 */
	public boolean hasErrors() { return hasErrors; };

	/******************************** THE VERILOG SCANNER ********************************/

	/**
	 * Method to do lexical scanning of input Verilog and create token list.
	 */
	private void doScanner(String [] strings)
	{
		String buf = "";
		int bufPos = 0;
		int lineNum = 0;
		boolean space = false;
		for(;;)
		{
			if (bufPos >= buf.length())
			{
				if (lineNum >= strings.length) return;
				buf = strings[lineNum++];
				if ((lineNum%100) == 0)
					Job.getUserInterface().setProgressValue( lineNum * 100 / strings.length);
				bufPos = 0;
				space = true;
			} else
			{
				if (Character.isWhitespace(buf.charAt(bufPos))) space = true; else
					space = false;
			}
			while (bufPos < buf.length() && Character.isWhitespace(buf.charAt(bufPos))) bufPos++;
			if (bufPos >= buf.length()) continue;
			char c = buf.charAt(bufPos);
			if (Character.isLetter(c))
			{
				// could be identifier (keyword) or bit string literal
				int end = bufPos;
				for(; end < buf.length(); end++)
				{
					char eChar = buf.charAt(end);
					if (!Character.isLetterOrDigit(eChar) && eChar != '_') break;
				}

				// got alphanumeric from c to end - 1
				VKeyword key = VKeyword.findKeyword(buf.substring(bufPos, end));
				if (key != null)
				{
					new TokenList(TokenType.KEYWORD, key, lineNum, space);
				} else
				{
					String ident = buf.substring(bufPos, end);
					new TokenList(TokenType.IDENTIFIER, ident, lineNum, space);
				}
				bufPos = end;
			} else if (TextUtils.isDigit(c))
			{
				// could be decimal or based literal
				int end = bufPos+1;
				for(; end < buf.length(); end++)
				{
					char eChar = buf.charAt(end);
					if (!TextUtils.isDigit(eChar) && eChar != '_') break;
				}

				// got numeric from c to end - 1
				new TokenList(TokenType.DECIMAL, buf.substring(bufPos, end), lineNum, space);
				bufPos = end;
			} else
			{
				switch (c)
				{
					case '\\':
						// backslash starts a quoted identifier
						int end = bufPos + 1;
						while (end < buf.length() && buf.charAt(end) != '\n')
						{
							if (Character.isWhitespace(buf.charAt(end))) break;
							end++;
						}
						// identifier from c + 1 to end - 1
						String ident = buf.substring(bufPos + 1, end);
						new TokenList(TokenType.IDENTIFIER, ident, lineNum, space);
						bufPos = end;
						break;
					case '/':
						// got a slash...look for "//" comment
						end = bufPos + 1;
						if (end >= buf.length() || buf.charAt(end) != '/')
						{
							new TokenList(TokenType.SLASH, null, lineNum, space);
							bufPos++;
							break;
						}

						// comment: skip to end of line
						while (end < buf.length() && buf.charAt(end) != '\n')
							end++;
						if (end < buf.length() && buf.charAt(end) == '\n') end++;
						bufPos = end;
						break;
					case '"':
						// got a start of a string
						end = bufPos + 1;
						while (end < buf.length() && buf.charAt(end) != '\n')
						{
							if (buf.charAt(end) == '"')
							{
								if (end+1 < buf.length() && buf.charAt(end+1) == '"') end++; else
									break;
							}
							end++;
						}
						// string from c + 1 to end - 1
						String newString = buf.substring(bufPos + 1, end);
						newString.replaceAll("\"\"", "\"");
						new TokenList(TokenType.STRING, newString, lineNum, space);
						if (buf.charAt(end) == '"') end++;
						bufPos = end;
						break;
					case '\'':
						// character literal
						if (bufPos+2 < buf.length() && buf.charAt(bufPos+2) == '\'')
						{
							new TokenList(TokenType.CHAR, new Character(buf.charAt(bufPos+1)), lineNum, space);
							bufPos += 3;
						} else
							bufPos++;
						break;
					case '(':
						new TokenList(TokenType.LEFTPAREN, null, lineNum, space);
						bufPos++;
						break;
					case ')':
						new TokenList(TokenType.RIGHTPAREN, null, lineNum, space);
						bufPos++;
						break;
					case '[':
						new TokenList(TokenType.LEFTBRACKET, null, lineNum, space);
						bufPos++;
						break;
					case ']':
						new TokenList(TokenType.RIGHTBRACKET, null, lineNum, space);
						bufPos++;
						break;
					case '{':
						new TokenList(TokenType.LEFTBRACE, null, lineNum, space);
						bufPos++;
						break;
					case '}':
						new TokenList(TokenType.RIGHTBRACE, null, lineNum, space);
						bufPos++;
						break;
					case ',':
						new TokenList(TokenType.COMMA, null, lineNum, space);
						bufPos++;
						break;
					case '-':
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '-')
						{
							// got a comment, throw away rest of line
							bufPos = buf.length();
						} else
						{
							// got a minus sign
							new TokenList(TokenType.MINUS, null, lineNum, space);
							bufPos++;
						}
						break;
					case '.':
						// could be PERIOD or DOUBLEDOT
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '.')
						{
							new TokenList(TokenType.DOUBLEDOT, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TokenType.PERIOD, null, lineNum, space);
							bufPos++;
						}
						break;
					case ':':
						// could be COLON or VARASSIGN
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '=')
						{
							new TokenList(TokenType.VARASSIGN, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TokenType.COLON, null, lineNum, space);
							bufPos++;
						}
						break;
					case ';':
						new TokenList(TokenType.SEMICOLON, null, lineNum, space);
						bufPos++;
						break;
					default:
						new TokenList(TokenType.UNKNOWN, null, lineNum, space);
						bufPos++;
						break;
				}
			}
		}
	}

	/******************************** THE VERILOG PARSER ********************************/

	/**
	 * Method to parse the token list.
	 * Reports on any syntax errors and create the required syntax trees.
	 */
	private void doParser()
	{
		curModule = null;
		resetTokenListPointer();
		int tokenCount = 0;
		for(;;)
		{
			TokenList token = getNextToken();
			if (token == null) break;

			tokenCount++;
			if ((tokenCount%100) == 0)
				Job.getUserInterface().setProgressValue( tokenIndex * 100 / tList.size());

			if (token.type == TokenType.KEYWORD)
			{
				VKeyword vk = (VKeyword)token.pointer;
				if (vk == VKeyword.MODULE)
				{
					curModule = parseModule();
					continue;
				}
				if (vk == VKeyword.ENDMODULE)
				{
					curModule = null;
					continue;
				}
				if (vk == VKeyword.INPUT || vk == VKeyword.OUTPUT ||
					vk == VKeyword.INOUT || vk == VKeyword.WIRE ||
					vk == VKeyword.SUPPLY || vk == VKeyword.SUPPLY0)
				{
					parseDeclare(token);
					continue;
				}
				if (vk == VKeyword.ASSIGN)
				{
					parseAssign(token);
					continue;
				}
				reportErrorMsg(token, "Unknown keyword");
			} else if (token.type == TokenType.IDENTIFIER)
			{
				// identifier: parse as an instance declaration
				if (curModule == null)
				{
					reportErrorMsg(token, "Instance declaration is not inside a Module");
					parseToSemicolon();
					break;
				}
				Instance inst = parseInstance(token);
				if (inst != null) curModule.instances.add(inst);
			} else
			{
				reportErrorMsg(token, "Expecting an identifier");
				parseToSemicolon();
			}
		}

		// fill in ports for modules that were not generated
		for(VModule module : allModules)
		{
			for(Instance in : module.instances)
			{
				for(LocalPort lp : in.ports.keySet())
				{
					boolean found = false;
					for(FormalPort subPort : in.module.ports)
					{
						if (subPort.name.equals(lp.portName)) { found = true;  break; }
					}
					if (!found)
					{
						FormalPort fp = new FormalPort(lp.portName);
						in.module.ports.add(fp);
					}
				}
			}
		}
	}

	/**
	 * Method to parse a module description of the form:
	 *    module IDENTIFIER (FORMAL_PORT_LIST);
	 *    FORMAL_PORT_LIST ::= [IDENTIFIER {, IDENTIFIER}]
	 */
	private VModule parseModule()
	{
		// check for entity IDENTIFIER
		TokenList token = needNextToken(TokenType.IDENTIFIER);
		if (token == null) return null;
		String name = (String)token.pointer;
		VModule module = findModule(name);
		if (module != null)
		{
			reportErrorMsg(token, "Module already exists");
			parseToSemicolon();
			return null;
		}
		module = new VModule(name, true);

		// check for opening bracket of FORMAL_PORT_LIST
		token = needNextToken(TokenType.LEFTPAREN);
		if (token == null) return null;

		// gather FORMAL_PORT_LIST
		for(;;)
		{
			token = needNextToken(TokenType.IDENTIFIER);
			if (token == null) return null;
			FormalPort port = new FormalPort((String)token.pointer);
			module.ports.add(port);

			token = getNextToken();
			if (getTokenType(token) != TokenType.COMMA) break;
		}

		// check for closing bracket of FORMAL_PORT_LIST
		if (getTokenType(token) != TokenType.RIGHTPAREN)
		{
			reportErrorMsg(token, "Expecting a right parenthesis");
			parseToSemicolon();
			return null;
		}

		// check for SEMICOLON
		token = needNextToken(TokenType.SEMICOLON);
		if (token == null) return null;

		return module;
	}

	private void parseAssign(TokenList token)
	{
		parseToSemicolon();
		return;		
	}

	private void parseDeclare(TokenList declareToken)
	{
		if (curModule == null)
		{
			reportErrorMsg(declareToken, "Not in a module");
			parseToSemicolon();
			return;
		}
		int mode = MODE_IN;
		VKeyword vk = (VKeyword)declareToken.pointer;
		if (vk == VKeyword.OUTPUT) mode = MODE_OUT; else 
			if (vk == VKeyword.INOUT) mode = MODE_INOUT;
		TokenList token = getNextToken();
		int firstRange = -1, secondRange = -1;
		if (getTokenType(token) == TokenType.LEFTBRACKET)
		{
			// a bus of bits
			token = getNextToken();
			firstRange = TextUtils.atoi((String)token.pointer);

			token = needNextToken(TokenType.COLON);
			if (token == null) return;

			token = getNextToken();
			secondRange = TextUtils.atoi((String)token.pointer);

			token = needNextToken(TokenType.RIGHTBRACKET);
			if (token == null) return;
			token = getNextToken();
		}

		// now get the list of identifiers
		for(;;)
		{
			if (getTokenType(token) != TokenType.IDENTIFIER)
			{
				reportErrorMsg(token, "Expected identifier");
				parseToSemicolon();
				return;
			}
			String idName = (String)token.pointer;
			boolean found = false;
			if (vk == VKeyword.WIRE || vk == VKeyword.SUPPLY || vk == VKeyword.SUPPLY0)
			{
				if (firstRange != -1 && secondRange != -1)
				{
					if (firstRange > secondRange) { int swap = firstRange;  firstRange = secondRange;  secondRange = swap; }
					for(int i=firstRange; i<=secondRange; i++)
					{
						String realName = idName + "[" + i + "]";
						if (curModule.wires.contains(realName))
						{
							reportErrorMsg(token, "Identifier " + realName + " defined twice");
							parseToSemicolon();
							return;
						}
						curModule.wires.add(realName);
					}
				} else
				{
					if (curModule.wires.contains(idName))
					{
						reportErrorMsg(token, "Identifier defined twice");
						parseToSemicolon();
						return;
					}
					curModule.wires.add(idName);
				}
			} else
			{
				for(FormalPort fp : curModule.ports)
				{
					if (fp.name.equals(idName))
					{
						fp.mode = mode;
						fp.firstIndex = firstRange;
						fp.secondIndex = secondRange;
						found = true;
						break;
					}
				}
				if (!found)
				{
					reportErrorMsg(token, "Unknown identifier");
					parseToSemicolon();
					return;
				}
			}

			// look for comma
			token = getNextToken();
			if (getTokenType(token) == TokenType.COMMA)
			{
				token = getNextToken();
				continue;
			}
			if (getTokenType(token) == TokenType.SEMICOLON) break;
			reportErrorMsg(token, "Unknown separator between identifiers");
			parseToSemicolon();
			return;
		}
	}

	private Instance parseInstance(TokenList token)
	{
		// get the cell name from the first token
		String cellName = (String)token.pointer;

		// get the instance name from the second token
		token = needNextToken(TokenType.IDENTIFIER);
		if (token == null) return null;
		String instanceName = (String)token.pointer;

		// must then be followed by an open parenthesis to start the argument list
		token = needNextToken(TokenType.LEFTPAREN);
		if (token == null) return null;

		VModule module = findModule(cellName);
		if (module == null)
			module = new VModule(cellName, false);
		Instance inst = new Instance(module, instanceName);

		// get the arguments
		int argNum = 1;
		for(;;)
		{
			token = getNextToken();
			if (getTokenType(token) == TokenType.RIGHTPAREN) break;

			// guess at the name of the next port
			String portName = "ARG" + argNum;
			argNum++;
			String signalName = null;
			if (getTokenType(token) == TokenType.PERIOD)
			{
				token = needNextToken(TokenType.IDENTIFIER);
				if (token == null) return null;
				portName = (String)token.pointer;

				token = needNextToken(TokenType.LEFTPAREN);
				if (token == null) return null;

				// either an identifier or a group of identifiers enclosed in braces
				token = getNextToken();
				if (getTokenType(token) == TokenType.LEFTBRACE)
				{
					int index = 0;
					if (inst.module.cell != null)
					{
						int lowestIndex = Integer.MAX_VALUE;
						int portNameLen = portName.length();
						for(Iterator<Export> it = inst.module.cell.getExports(); it.hasNext(); )
						{
							Export e = it.next();
							if (e.getName().startsWith(portName) && e.getName().length() > portNameLen)
							{
								if (e.getName().charAt(portNameLen) == '[')
								{
									int thisIndex = TextUtils.atoi(e.getName().substring(portNameLen+1));
									if (thisIndex < lowestIndex) lowestIndex = thisIndex;
								}
							}
						}
						index = lowestIndex;
					}
					for(;;)
					{
						token = getNextToken();
						if (getTokenType(token) != TokenType.IDENTIFIER)
						{
							reportErrorMsg(token, "Expecting an identifier");
							parseToSemicolon();
							return null;
						}
						signalName = getSignalName(token);
						inst.addPort(new LocalPort(inst, portName + "[" + index + "]"), signalName);
						index++;

						token = getNextToken();
						if (getTokenType(token) == TokenType.RIGHTBRACE) break;
						if (getTokenType(token) != TokenType.COMMA)
						{
							reportErrorMsg(token, "Expecting a comma");
							parseToSemicolon();
							return null;
						}
					}
				} else
				{
					// single port
					if (getTokenType(token) != TokenType.IDENTIFIER)
					{
						reportErrorMsg(token, "Expecting an identifier");
						parseToSemicolon();
						return null;
					}

					signalName = getSignalName(token);
					inst.addPort(new LocalPort(inst, portName), signalName);
				}
				token = needNextToken(TokenType.RIGHTPAREN);
				if (token == null) return null;
			} else if (getTokenType(token) == TokenType.IDENTIFIER)
			{
				signalName = getSignalName(token);
				inst.addPort(new LocalPort(inst, portName), signalName);
			} else
			{
				reportErrorMsg(token, "Unknown separator between identifiers");
				parseToSemicolon();
				return null;
			}
			token = getNextToken();
			if (getTokenType(token) == TokenType.RIGHTPAREN) break;
			if (getTokenType(token) != TokenType.COMMA)
			{
				reportErrorMsg(token, "Expecting a comma");
				parseToSemicolon();
				return null;
			}
		}

		// must end with a semicolon
		token = needNextToken(TokenType.SEMICOLON);
		if (token == null) return null;
		return inst;
	}

	private String getSignalName(TokenList token)
	{
		if (getTokenType(token) != TokenType.IDENTIFIER) return null;
		String signalName = (String)token.pointer;

		// see if it is indexed
		TokenList next = peekNextToken();
		if (getTokenType(next) == TokenType.LEFTBRACKET)
		{
			// indexed signal
			getNextToken();
			TokenList index = needNextToken(TokenType.DECIMAL);
			if (index == null) return null;
			TokenList close = needNextToken(TokenType.RIGHTBRACKET);
			if (close == null) return null;
			signalName += "[" + (String)index.pointer + "]";			
		}
		return signalName;
	}

	/**
	 * Method to ignore up to the next semicolon.
	 */
	private void parseToSemicolon()
	{
		for(;;)
		{
			TokenList token = getNextToken();
			if (token.type == TokenType.SEMICOLON) break;
		}
	}

	private void reportErrorMsg(TokenList tList, String errMsg)
	{
		hasErrors = true;
		errorCount++;
		if (errorCount == 30)
			System.out.println("TOO MANY ERRORS...PRINTING NO MORE");
		if (errorCount >= 30) return;
		if (tList == null)
		{
			System.out.println("ERROR " + errMsg);
			return;
		}
		System.out.println("ERROR on line " + tList.lineNum + ", " + errMsg + ":");

		// back up to start of line
		StringBuffer buffer = new StringBuffer();
		int pointer = tList.makeErrorLine(buffer);

		// print out line
		System.out.println(buffer.toString());

		// print out pointer
		buffer = new StringBuffer();
		for (int i = 0; i < pointer; i++) buffer.append(" ");
		System.out.println(buffer.toString() + "^");
	}

	/******************************** THE RATS-NEST CELL GENERATOR ********************************/

	/**
	 * Method to generate a cell that represents this netlist.
	 * @param destLib destination library.
	 */
	public Cell genCell(Library destLib)
	{
		if (hasErrors) return null;
		Map<NodeProto,Map<PortProto,Point2D>> portLocMap = new HashMap<NodeProto,Map<PortProto,Point2D>>();

		Job.getUserInterface().startProgressDialog("Building Rats-Nest Cells", null);

		for(VModule mod : allModules)
		{
			if (!mod.defined) continue;
			String cellName = mod.name + "{lay}";
			System.out.println("Creating cell " + cellName);
			Job.getUserInterface().setProgressNote("Creating Nodes in Cell " + cellName);
			Job.getUserInterface().setProgressValue(0);
			Cell cell = Cell.makeInstance(destLib, cellName);
			if (cell == null)
			{
				Job.getUserInterface().stopProgressDialog();
				return null;
			}

			// first place the instances
			double GAP = 5;
			double x = 0;
			double y = 0;
			double highest = 0;
			double totalSize = 0;
			for(Instance in : mod.instances)
			{
				Cell subCell = in.module.cell;
				if (subCell == null) continue;
				double width = subCell.getDefWidth();
				double height = subCell.getDefHeight();
				totalSize += (width + GAP) * (height + GAP);
			}
			double cellSize = Math.sqrt(totalSize);
			Map<Instance,NodeInst> placed = new HashMap<Instance,NodeInst>();
			int instancesPlaced = 0;
			for(Instance in : mod.instances)
			{
				Cell subCell = in.module.cell;
				if (subCell == null) continue;
				double width = subCell.getDefWidth();
				double height = subCell.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(subCell, new EPoint(x, y), width, height, cell);
				if (ni == null) continue;
				placed.put(in, ni);

				// cache port locations on this instance
				Map<PortProto,Point2D> portMap = portLocMap.get(subCell);
				if (portMap == null)
				{
					portMap = new HashMap<PortProto,Point2D>();
					portLocMap.put(subCell, portMap);
					for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
					{
						PortInst pi = it.next();
						PortProto pp = pi.getPortProto();
						EPoint ept = pi.getCenter();
						Point2D pt = new Point2D.Double(ept.getX() - ni.getAnchorCenterX(), ept.getY() - ni.getAnchorCenterY());
						portMap.put(pp, pt);
					}
				}
				instancesPlaced++;
				if ((instancesPlaced%100) == 0)
					Job.getUserInterface().setProgressValue(instancesPlaced * 100 / mod.instances.size());

				// advance the placement
				x += width + GAP;
				highest = Math.max(highest, height);
				if (x >= cellSize)
				{
					x = 0;
					y += highest + GAP;
					highest = 0;
				}
			}
			System.out.println("Placed " + mod.instances.size() + " cell instances");

			// now remove wires that do not make useful connections
			Netlist nl = cell.getNetlist();
			for(String netName : mod.allNetworks.keySet())
			{
				List<LocalPort> ports = mod.allNetworks.get(netName);
				Set<Network> used = new HashSet<Network>();
				for(int i=0; i<ports.size(); i++)
				{
					LocalPort lp = ports.get(i);
					NodeInst ni = placed.get(lp.in);
                    if (ni == null)
                    {
                        hasErrors = true;
                        System.out.println("NodeInst " + lp.in.instanceName + " not found.");
                        System.out.println("Check errors reported.");
                		Job.getUserInterface().stopProgressDialog();
                        return null; // stop here the execution.
                    }
					PortInst pi = ni.findPortInst(lp.portName);
					Network net = nl.getNetwork(pi);
					if (used.contains(net))
					{
						ports.remove(i);
						i--;
						continue;
					}
					used.add(net);
				}
			}

			// now wire the instances
			Job.getUserInterface().setProgressNote("Creating Arcs in Cell " + cellName);
			Job.getUserInterface().setProgressValue(0);
			int total = 0;
			for(String netName : mod.allNetworks.keySet())
			{
				List<LocalPort> ports = mod.allNetworks.get(netName);
				for(int i=1; i<ports.size(); i++)
					total++;
			}

			// cache information for creating unrouted arcs
			ArcProto ap = Generic.tech().unrouted_arc;
	        EditingPreferences ep = cell.getEditingPreferences();
	        ImmutableArcInst a = ap.getDefaultInst(ep);
	        long gridExtendOverMin = DBMath.lambdaToGrid(0.5 * ap.getDefaultLambdaBaseWidth(ep)) - ap.getGridBaseExtend();
	        TextDescriptor nameDescriptor = TextDescriptor.getArcTextDescriptor();

			int count = 0;
	        for(String netName : mod.allNetworks.keySet())
			{
				List<LocalPort> ports = mod.allNetworks.get(netName);
				for(int i=1; i<ports.size(); i++)
				{
					LocalPort fromPort = ports.get(i-1);
					LocalPort toPort = ports.get(i);
					NodeInst fromNi = placed.get(fromPort.in);
					NodeInst toNi = placed.get(toPort.in);
					PortInst fromPi = fromNi.findPortInst(fromPort.portName);
					PortInst toPi = toNi.findPortInst(toPort.portName);
					EPoint fromCtr = getPortCenter(fromPi, portLocMap);
					EPoint toCtr = getPortCenter(toPi, portLocMap);
				    ArcInst.newInstanceNoCheck(cell, ap, netName, nameDescriptor, fromPi, toPi,
				    	fromCtr, toCtr, gridExtendOverMin, ArcInst.DEFAULTANGLE, a.flags);
//					ArcInst.makeInstance(ap, fromPi, toPi, fromCtr, toCtr, netName);
					count++;
					if ((count%100) == 0)
						Job.getUserInterface().setProgressValue(count * 100 / total);
					netName = null;
				}
			}
			System.out.println("Created " + count + " wires");
			Job.getUserInterface().stopProgressDialog();
			return cell;
		}
		Job.getUserInterface().stopProgressDialog();
		return null;
	}

	/**
	 * Method to cache the center of ports.
	 * This is necessary because the call to "PortInst.getCenter()" is expensive.
	 * This method assumes that all nodes are unrotated.
	 * @param pi the PortInst being requested.
	 * @param portLocMap a caching map for port locations.
	 * @return the center of the PortInst.
	 */
	private EPoint getPortCenter(PortInst pi, Map<NodeProto,Map<PortProto,Point2D>> portLocMap)
	{
		NodeInst ni = pi.getNodeInst();
		Map<PortProto,Point2D> portMap = portLocMap.get(ni.getProto());
		Point2D pt = portMap.get(pi.getPortProto());
		return new EPoint(pt.getX() + ni.getAnchorCenterX(), pt.getY() + ni.getAnchorCenterY());
	}

	/******************************** THE ALS NETLIST GENERATOR ********************************/

	/**
	 * Method to generate an ALS (simulation) netlist.
	 * @param destLib destination library.
	 * @return a List of strings with the netlist.
	 */
	public List<String> getALSNetlist(Library destLib)
	{
		// now produce the netlist
		if (hasErrors) return null;
		List<String> netlistStrings = genALS(destLib, null);
		return netlistStrings;
	}

	/**
	 * Method to generate ALS target output for the created parse tree.
	 * Assume parse tree is semantically correct.
	 * @param destLib destination library.
	 * @param behaveLib behavior library.
	 * @return a list of strings that has the netlist.
	 */
	private List<String> genALS(Library destLib, Library behaveLib)
	{
		// print file header
		List<String> netlist = new ArrayList<String>();
		netlist.add("#*************************************************");
		netlist.add("#  ALS Netlist file");
		netlist.add("#");
		if (User.isIncludeDateAndVersionInOutput())
			netlist.add("#  File Creation:    " + TextUtils.formatDate(new Date()));
		netlist.add("#*************************************************");
		netlist.add("");

		// determine top level cell
		for(VModule mod : allModules)
		{
			if (mod.defined)
				genALSInterface(mod, netlist);
		}

		// print closing line of output file
		netlist.add("#********* End of netlist *******************");
		return netlist;
	}

	/**
	 * Method to generate the ALS description for the specified model.
	 * @param module module to analyze
	 * @param netlist the List of strings to create.
	 */
	private void genALSInterface(VModule module, List<String> netlist)
	{
		// write this entity
		String modLine = "model " + module.name + "(";
		boolean first = true;
		for(FormalPort fp : module.ports)
		{
			for(int i=fp.firstIndex; i<=fp.secondIndex; i++)
			{
				if (!first) modLine += ", ";
				first = false;
				modLine += fp.name;
				if (i != -1) modLine += "_" + i + "_";
			}
		}
		modLine += ")";
		netlist.add(modLine);

		// write instances
		for(Instance in : module.instances)
		{
			first = true;
			String inName = in.instanceName.replaceAll("/", "_").replaceAll("\\[", "_").replaceAll("\\]", "_");
			String inLine = inName + ": " + in.module.name + "(";
			for(LocalPort lp : in.ports.keySet())
			{
				if (!first) inLine += ", ";
				first = false;
				String name = in.ports.get(lp).replaceAll("/", "_").replaceAll("\\[", "_").replaceAll("\\]", "_");
				inLine += name;
			}
			inLine += ")";
			netlist.add(inLine);
		}
		netlist.add("");
	}

	/******************************** THE QUISC NETLIST GENERATOR ********************************/

	/**
	 * Method to generate a QUISC (silicon compiler) netlist.
	 * @param destLib destination library.
     * @param isIncldeDateAndVersionInOutput include date and version in output
	 * @return a List of strings with the netlist.
	 */
	public List<String> getQUISCNetlist(Library destLib, boolean isIncludeDateAndVersionInOutput)
	{
		// now produce the netlist
		if (hasErrors) return null;
		List<String> netlist = new ArrayList<String>();

		// print file header
		netlist.add("!*************************************************");
		netlist.add("!  QUISC Command file");
		netlist.add("!");
		if (isIncludeDateAndVersionInOutput)
			netlist.add("!  File Creation:    " + TextUtils.formatDate(new Date()));
		netlist.add("!-------------------------------------------------");
		netlist.add("");

		// determine top level cell
		for(VModule mod : allModules)
		{
			if (mod.defined)
				genQuiscInterface(mod, netlist);
		}

		// print closing line of output file
		netlist.add("!********* End of command file *******************");
		return netlist;
	}

	/**
	 * Method to generate the QUISC description for the specified model.
	 * @param module module to analyze
	 * @param netlist the List of strings to create.
	 */
	private void genQuiscInterface(VModule module, List<String> netlist)
	{
		// write this entity
		netlist.add("create cell " + module.name);

		// write instances
		for(Instance in : module.instances)
		{
			netlist.add("create instance " + in.instanceName + " " + in.module.name);
		}

		for(String netName : module.allNetworks.keySet())
		{
			List<LocalPort> ports = module.allNetworks.get(netName);
			LocalPort last = null;
			for(LocalPort lp : ports)
			{
				if (last != null)
					netlist.add("connect " + last.in.instanceName + " " + last.portName + " " + lp.in.instanceName + " " + lp.portName);
				last = lp;
			}
		}

		// create export list
		for(FormalPort port : module.ports)
		{
			for(int i = port.firstIndex; i<=port.secondIndex; i++)
			{
				String name = port.name;
				if (i != -1) name += "[" + i + "]";
				boolean found = false;
				for(Instance in : module.instances)
				{
					for(LocalPort lp : in.ports.keySet())
					{
						String lpName = in.ports.get(lp);
						if (lpName.equals(name))
						{
							String line = "export " + in.instanceName + " " + lp.portName + " " + name + " ";
							switch (port.mode)
							{
								case MODE_UNKNOWN: line += "unknown";  break;
								case MODE_IN:      line += "input";    break;
								case MODE_OUT:     line += "output";   break;
								case MODE_INOUT:   line += "inout";    break;
							}
							netlist.add(line);
							found = true;
							break;
						}
					}
					if (found) break;
				}
				if (!found)
					netlist.add("! DID NOT FIND EXPORT "+name);
			}
		}
	}
}
