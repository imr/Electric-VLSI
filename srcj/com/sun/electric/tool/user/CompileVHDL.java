/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CompileVHDL.java
 * Compile VHDL to a netlist
 * Written by Andrew R. Kostiuk, Queen's University.
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.sc.SilComp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This is the VHDL Compiler.
 */
public class CompileVHDL
{
	/********** Token Definitions ******************************************/

	/********** Delimiters **********/
	private static final int TOKEN_AMPERSAND	= 0;
	private static final int TOKEN_APOSTROPHE	= 1;
	private static final int TOKEN_LEFTBRACKET	= 2;
	private static final int TOKEN_RIGHTBRACKET	= 3;
	private static final int TOKEN_STAR			= 4;
	private static final int TOKEN_PLUS			= 5;
	private static final int TOKEN_COMMA		= 6;
	private static final int TOKEN_MINUS		= 7;
	private static final int TOKEN_PERIOD		= 8;
	private static final int TOKEN_SLASH		= 9;
	private static final int TOKEN_COLON		= 10;
	private static final int TOKEN_SEMICOLON	= 11;
	private static final int TOKEN_LT			= 12;
	private static final int TOKEN_EQ			= 13;
	private static final int TOKEN_GT			= 14;
	private static final int TOKEN_VERTICALBAR	= 15;
	/********** Compound Delimiters **********/
	private static final int TOKEN_ARROW		= 16;
	private static final int TOKEN_DOUBLEDOT	= 17;
	private static final int TOKEN_DOUBLESTAR	= 18;
	private static final int TOKEN_VARASSIGN	= 19;
	private static final int TOKEN_NE			= 20;
	private static final int TOKEN_GE			= 21;
	private static final int TOKEN_LE			= 22;
	private static final int TOKEN_BOX			= 23;
	/********** Other Token **********/
	private static final int TOKEN_UNKNOWN		= 24;
	private static final int TOKEN_IDENTIFIER	= 25;				/* alphanumeric (first char alpha) */
	private static final int TOKEN_KEYWORD		= 26;				/* reserved keyword of the language */
	private static final int TOKEN_DECIMAL		= 27;				/* decimal literal */
	private static final int TOKEN_BASED		= 28;				/* based literal */
	private static final int TOKEN_CHAR			= 29;				/* character literal */
	private static final int TOKEN_STRING		= 30;				/* string enclosed in double quotes */
	private static final int TOKEN_BIT_STRING	= 31;				/* bit string */

	/********** Keyword Constants ******************************************/

	private static final int KEY_ABS			= 0;
	private static final int KEY_AFTER			= 1;
	private static final int KEY_ALIAS			= 2;
	private static final int KEY_AND			= 3;
	private static final int KEY_ARCHITECTURE	= 4;
	private static final int KEY_ARRAY			= 5;
	private static final int KEY_ASSERTION		= 6;
	private static final int KEY_ATTRIBUTE		= 7;
	private static final int KEY_BEHAVIORAL		= 8;
	private static final int KEY_BEGIN			= 9;
	private static final int KEY_BODY			= 10;
	private static final int KEY_CASE			= 11;
	private static final int KEY_COMPONENT		= 12;
	private static final int KEY_CONNECT		= 13;
	private static final int KEY_CONSTANT		= 14;
	private static final int KEY_CONVERT		= 15;
	private static final int KEY_DOT			= 16;
	private static final int KEY_DOWNTO			= 17;
	private static final int KEY_ELSE			= 18;
	private static final int KEY_ELSIF			= 19;
	private static final int KEY_END			= 20;
	private static final int KEY_ENTITY			= 21;
	private static final int KEY_EXIT			= 22;
	private static final int KEY_FOR			= 23;
	private static final int KEY_FUNCTION		= 24;
	private static final int KEY_GENERATE		= 25;
	private static final int KEY_GENERIC		= 26;
	private static final int KEY_IF				= 27;
	private static final int KEY_IN				= 28;
	private static final int KEY_INOUT			= 29;
	private static final int KEY_IS				= 30;
	private static final int KEY_LINKAGE		= 31;
	private static final int KEY_LOOP			= 32;
	private static final int KEY_MOD			= 33;
	private static final int KEY_NAND			= 34;
	private static final int KEY_NEXT			= 35;
	private static final int KEY_NOR			= 36;
	private static final int KEY_NOT			= 37;
	private static final int KEY_NULL			= 38;
	private static final int KEY_OF				= 39;
	private static final int KEY_OR				= 40;
	private static final int KEY_OTHERS			= 41;
	private static final int KEY_OUT			= 42;
	private static final int KEY_PACKAGE		= 43;
	private static final int KEY_PORT			= 44;
	private static final int KEY_RANGE			= 45;
	private static final int KEY_RECORD			= 46;
	private static final int KEY_REM			= 47;
	private static final int KEY_REPORT			= 48;
	private static final int KEY_RESOLVE		= 49;
	private static final int KEY_RETURN			= 50;
	private static final int KEY_SEVERITY		= 51;
	private static final int KEY_SIGNAL			= 52;
	private static final int KEY_STANDARD		= 53;
	private static final int KEY_STATIC			= 54;
	private static final int KEY_SUBTYPE		= 55;
	private static final int KEY_THEN			= 56;
	private static final int KEY_TO				= 57;
	private static final int KEY_TYPE			= 58;
	private static final int KEY_UNITS			= 59;
	private static final int KEY_USE			= 60;
	private static final int KEY_VARIABLE		= 61;
	private static final int KEY_WHEN			= 62;
	private static final int KEY_WHILE			= 63;
	private static final int KEY_WITH			= 64;
	private static final int KEY_XOR			= 65;
	private static final int KEY_OPEN			= 66;
	private static final int KEY_MAP			= 67;
	private static final int KEY_ALL			= 68;
	private static final int KEY_LIBRARY		= 69;

	/********** Keyword Structures *****************************************/

	private static class VKEYWORD
	{
		String	name;							/* string defining keyword */
		int		num;							/* number of keyword */

		VKEYWORD(String name, int num) { this.name = name;   this.num = num; }
	};

	/********** Token Structures *****************************************/

	private static class TOKENLIST
	{
		int	token;							/* token number */
		Object	pointer;						/* NULL if delimiter, */
												/* pointer to global name space if identifier, */
												/* pointer to keyword table if keyword, */
												/* pointer to string if decimal literal, */
												/* pointer to string if based literal, */
												/* value of character if character literal, */
												/* pointer to string if string literal, */
												/* pointer to string if bit string literal */
		boolean	space;							/* TRUE if space before next token */
		int	line_num;						/* line number token occurred */
		TOKENLIST next;				/* next in list */
		TOKENLIST last;				/* previous in list */
	};

	/********** Symbol Trees **********************************************/

	private static final int SYMBOL_ENTITY		= 1;
	private static final int SYMBOL_BODY		= 2;
	private static final int SYMBOL_TYPE		= 3;
	private static final int SYMBOL_FPORT		= 4;
	private static final int SYMBOL_COMPONENT	= 5;
	private static final int SYMBOL_SIGNAL		= 6;
	private static final int SYMBOL_INSTANCE	= 7;
	private static final int SYMBOL_VARIABLE	= 8;
	private static final int SYMBOL_LABEL		= 9;
	private static final int SYMBOL_PACKAGE		= 10;
	private static final int SYMBOL_CONSTANT	= 11;

	private static class SYMBOLTREE
	{
		String			value;			/* identifier */
		int				type;			/* type of item */
		Object			pointer;		/* pointer to item */
		int             seen;			/* flag for deallocation */
	};

	private static class SYMBOLLIST
	{
		HashMap         sym;
		SYMBOLLIST		last;			/* previous in stack */
		SYMBOLLIST		next;			/* next in list */
	};

	/********** Gate Entity Structures *************************************/

	private static class GATE
	{
		String			name;			/* name of gate */
		String			header;			/* header line */
		GATELINE		lines;			/* lines of gate def'n */
		SYMBOLTREE		flags;			/* flags for general use */
		GATE			next;			/* next gate in list */
	};

	private static class GATELINE
	{
		SYMBOLLIST		line;			/* line of gate def'n */
		GATELINE		next;			/* next line in def'n */
	};

	/********** Unresolved Reference List **********************************/

	private static class UNRESLIST
	{
		String			interfacef;	/* name of reference */
		int				numref;			/* number of references */
		UNRESLIST		next;			/* next in list */
	};

	/********** ALS Generation Constants *********************************/

	private static final int TOP_ENTITY_FLAG	= 0x0001;			/* flag the entity as called */
	private static final int ENTITY_WRITTEN		= 0x0002;			/* flag the entity as written */

	/***********************************************************************/

	/*
	 * Header file for VHDL compiler including data base structures and constants.
	 */

	private static class DBUNITS
	{
		DBINTERFACE		interfaces;	/* list of interfaces */
		DBBODY			bodies;		/* list of bodies */
	};

	private static class DBPACKAGE
	{
		String			name;			/* name of package */
		SYMBOLLIST		root;			/* root of symbol tree */
	};

	private static class DBINTERFACE
	{
		String			name;			/* name of interface */
		DBPORTLIST		ports;			/* list of ports */
		Object			interfacef;	/* interface declarations */
		int				flags;			/* for later code gen */
		DBBODY			bodies;		/* associated bodies */
		SYMBOLLIST		symbols;		/* local symbols */
		DBINTERFACE		next;			/* next interface */
	};

	private static final int DBMODE_IN			= 1;
	private static final int DBMODE_OUT			= 2;
	private static final int DBMODE_DOTOUT		= 3;
	private static final int DBMODE_INOUT		= 4;
	private static final int DBMODE_LINKAGE		= 5;
	private static class DBPORTLIST
	{
		String			name;			/* name of port */
		int				mode;			/* mode of port */
		DBLTYPE			type;			/* type of port */
		int				flags;			/* general flags */
		DBPORTLIST		next;			/* next in port list */
	};

	private static final int DBTYPE_SINGLE		= 1;
	private static final int DBTYPE_ARRAY		= 2;
	private static class DBLTYPE
	{
		String			name;			/* name of type */
		int				type;			/* type of type */
		Object			pointer;		/* pointer to info */
		DBLTYPE			subtype;		/* possible subtype */
	};

	/********** Bodies *****************************************************/

	private static final int DBBODY_BEHAVIORAL		= 1;
	private static final int DBBODY_ARCHITECTURAL	= 2;
	private static class DBBODY
	{
		int				classnew;		/* class of body */
		String			name;			/* name of body - identifier */
		String			entity;		/* parent entity of body */
		DBBODYDECLARE	declare;		/* declarations */
		DBSTATEMENTS	statements;	/* statements in body */
		DBINTERFACE		parent;		/* pointer to parent */
		DBBODY			same_parent;	/* bodies of same parent */
		DBBODY			next;			/* next body */
	};

	private static class DBBODYDECLARE
	{
		DBCOMPONENTS	components;	/* components */
		DBSIGNALS		bodysignals;		/* signals */
	};

	private static class DBCOMPONENTS
	{
		String			name;			/* name of component */
		DBPORTLIST		ports;			/* list of ports */
		DBCOMPONENTS	next;			/* next component */
	};

	private static class DBSIGNALS
	{
		String			name;			/* name of signal */
		DBLTYPE			type;			/* type of signal */
		DBSIGNALS		next;			/* next signal */
	};

	/********** Architectural Statements ***********************************/

	private static class DBSTATEMENTS
	{
		DBINSTANCE		instances;
	};

	private static class DBINSTANCE
	{
		String			name;			/* identifier */
		DBCOMPONENTS	compo;			/* component */
		DBAPORTLIST		ports;			/* ports on instance */
		DBINSTANCE		next;			/* next instance in list */
	};

	private static class DBAPORTLIST
	{
		DBNAME			name;			/* name of port */
		DBPORTLIST		port;			/* pointer to port on comp */
		int				flags;			/* flags for processing */
		DBAPORTLIST		next;			/* next in list */
	};

	/********** Names ******************************************************/

	private static final int DBNAME_IDENTIFIER		= 1;
	private static final int DBNAME_INDEXED			= 2;
	private static final int DBNAME_CONCATENATED	= 3;
	private static class DBNAME
	{
		String			name;			/* name of name */
		int				type;			/* type of name */
		Object			pointer;		/* NULL if identifier */
									 			/* pointer to DBEXPRLIST if indexed */
									 			/* pointer to DBNAMELIST if concatenated */
		DBLTYPE			dbtype; 				/* pointer to type */
	};

	private static class DBEXPRLIST
	{
		int				value;			/* value */
		DBEXPRLIST		next;			/* next in list */
	};

	private static class DBDISCRETERANGE
	{
		int				start;			/* start of range */
		int				end;			/* end of range */
	};

	private static class DBINDEXRANGE
	{
		DBDISCRETERANGE	drange;		/* discrete range */
		DBINDEXRANGE	next;			/* next in list */
	};

	private static class DBNAMELIST
	{
		DBNAME			name;			/* name in list */
		DBNAMELIST		next;			/* next in list */
	};

	/***********************************************************************/

	/*
	 * Header file for VHDL compiler including parse tree structures and constants.
	 */

	/******** Parser Constants and Structures ******************************/

	private static final int NOUNIT			= 0;
	private static final int UNIT_INTERFACE	= 1;
	private static final int UNIT_FUNCTION	= 2;
	private static final int UNIT_PACKAGE	= 3;
	private static final int UNIT_BODY		= 4;
	private static final int UNIT_USE		= 6;
	private static class PTREE
	{
		int				type;			/* type of entity */
		Object			pointer;		/* pointer to design unit */
		PTREE			next;			/* pointer to next */
	};

	/********** Packages ***************************************************/

	private static class PACKAGE
	{
		TOKENLIST		name;			/* package name */
		PACKAGEDPART	declare;		/* package declare part */
	};

	private static class PACKAGEDPART
	{
		BASICDECLARE	item;			/* package declare item */
		PACKAGEDPART	next;			/* pointer to next */
	};

	private static class USE
	{
		TOKENLIST		unit;			/* unit */
		USE				next;			/* next in list */
	};

	/********** Interfaces *************************************************/

	private static class VINTERFACE
	{
		TOKENLIST		name;			/* name of entity */
		FPORTLIST		ports;			/* list of ports */
		Object			interfacef;	/* interface declarations */
	};

	private static final int MODE_IN			= 1;
	private static final int MODE_OUT			= 2;
	private static final int MODE_DOTOUT		= 3;
	private static final int MODE_INOUT			= 4;
	private static final int MODE_LINKAGE		= 5;
	private static class FPORTLIST
	{
		IDENTLIST		names;			/* names of port */
		int				mode;			/* mode of port */
		VNAME			type;			/* type of port */
		FPORTLIST		next;			/* next in port list */
	};

	private static class IDENTLIST
	{
		TOKENLIST		identifier;	/* identifier */
		IDENTLIST		next;			/* next in list */
	};

	/********** Bodies *****************************************************/

	private static final int BODY_BEHAVIORAL	= 1;
	private static final int BODY_ARCHITECTURAL	= 2;
	private static class BODY
	{
		int				classnew;		/* class of body */
		TOKENLIST		name;			/* name of body - identifier */
		SIMPLENAME		entity;		/* parent entity of body */
		BODYDECLARE		body_declare;	/* body declarations */
		STATEMENTS		statements;	/* statements in body */
	};

	private static final int BODYDECLARE_BASIC		= 1;
	private static final int BODYDECLARE_COMPONENT	= 2;
	private static final int BODYDECLARE_RESOLUTION	= 3;
	private static final int BODYDECLARE_LOCAL		= 4;
	private static class BODYDECLARE
	{
		int				type;			/* type of declaration */
		Object			pointer;		/* pointer to part tree */
		BODYDECLARE		next;			/* next in list */
	};

	/********** Basic Declarations *****************************************/

	private static final int NOBASICDECLARE				= 0;
	private static final int BASICDECLARE_OBJECT		= 1;
	private static final int BASICDECLARE_TYPE			= 2;
	private static final int BASICDECLARE_SUBTYPE		= 3;
	private static final int BASICDECLARE_CONVERSION	= 4;
	private static final int BASICDECLARE_ATTRIBUTE		= 5;
	private static final int BASICDECLARE_ATT_SPEC		= 6;
	private static class BASICDECLARE
	{
		int				type;			/* type of basic declare */
		Object			pointer;		/* pointer to parse tree */
	};

	private static final int NOOBJECTDECLARE			= 0;
	private static final int OBJECTDECLARE_CONSTANT		= 1;
	private static final int OBJECTDECLARE_SIGNAL		= 2;
	private static final int OBJECTDECLARE_VARIABLE		= 3;
	private static final int OBJECTDECLARE_ALIAS		= 4;
	private static class OBJECTDECLARE
	{
		int				type;			/* type of object declare */
		Object			pointer;		/* pointer to parse tree */
	};

	private static class SIGNALDECLARE
	{
		IDENTLIST		names;			/* list of identifiers */
		SUBTYPEIND		subtype;		/* subtype indicator */
	};

	private static class VCOMPONENT
	{
		TOKENLIST		name;			/* name of component */
		FPORTLIST		ports;			/* ports of component */
	};

	private static class CONSTANTDECLARE
	{
		TOKENLIST		identifier;	/* name of constant */
		SUBTYPEIND		subtype;		/* subtype indicator */
		EXPRESSION		expression;	/* expression */
	};

	/********** Types ******************************************************/

	private static class SUBTYPEIND
	{
		VNAME			type;			/* type of subtype */
		CONSTAINT		constraint;	/* optional constaint */
	};

	private static final int CONSTAINT_RANGE		= 1;
	private static final int CONSTAINT_FLOAT		= 2;
	private static final int CONSTAINT_INDEX		= 3;
	private static class CONSTAINT
	{
		int				type;			/* type of constaint */
		Object			pointer;		/* pointer to parse tree */
	};

	private static final int TYPE_SCALAR		= 1;
	private static final int TYPE_COMPOSITE		= 2;
	private static class TYPE
	{
		TOKENLIST		identifier;	/* name of type */
		int				type;			/* type definition */
		Object			pointer;		/* pointer to type */
	};

	private static final int COMPOSITE_ARRAY	= 1;
	private static final int COMPOSITE_RECORD	= 2;
	private static class COMPOSITE
	{
		int				type;			/* type of composite */
		Object			pointer;		/* pointer to composite */
	};

	private static final int ARRAY_UNCONSTRAINED	= 1;
	private static final int ARRAY_CONSTRAINED		= 2;
	private static class ARRAY
	{
		int				type;			/* (un)constrained array */
		Object			pointer;		/* pointer to array */
	};

	private static class CONSTRAINED
	{
		INDEXCONSTRAINT	constraint;	/* index constraint */
		SUBTYPEIND		subtype;		/* subtype indication */
	};

	private static class INDEXCONSTRAINT
	{
		DISCRETERANGE	discrete;		/* discrete range */
		INDEXCONSTRAINT	next;			/* possible more */
	};

	/********** Architectural Statements ***********************************/

	private static final int NOARCHSTATE			= 0;
	private static final int ARCHSTATE_GENERATE		= 1;
	private static final int ARCHSTATE_SIG_ASSIGN	= 2;
	private static final int ARCHSTATE_IF			= 3;
	private static final int ARCHSTATE_CASE			= 4;
	private static final int ARCHSTATE_INSTANCE		= 5;
	private static final int ARCHSTATE_NULL			= 6;
	private static class STATEMENTS
	{
		int				type;			/* type of statement */
		Object			pointer;		/* pointer to parse tree */
		STATEMENTS		next;			/* pointer to next */
	};

	private static class VINSTANCE
	{
		TOKENLIST		name;			/* optional identifier */
		SIMPLENAME		entity;		/* entity of instance */
		APORTLIST		ports;			/* ports on instance */
	};

	private static final int APORTLIST_NAME			= 1;
	private static final int APORTLIST_TYPE_NAME	= 2;
	private static final int APORTLIST_EXPRESSION	= 3;
	private static class APORTLIST
	{
		int				type;			/* type of actual port */
		Object			pointer;		/* pointer to parse tree */
		APORTLIST		next;			/* next in list */
	};

	private static class GENERATE
	{
		TOKENLIST		label;			/* optional label */
		GENSCHEME		gen_scheme;	/* generate scheme */
		STATEMENTS		statements;	/* statements */
	};

	private static final int GENSCHEME_FOR		= 0;
	private static final int GENSCHEME_IF		= 1;
	private static class GENSCHEME
	{
		int				scheme;			/* scheme (for or if) */
		TOKENLIST		identifier;	/* if FOR scheme */
		DISCRETERANGE	range;			/* if FOR scheme */
		EXPRESSION		condition;		/* if IF scheme */
	};

	private static class WAVEFORMLIST
	{
		WAVEFORM		waveform;		/* waveform element */
		WAVEFORMLIST	next;			/* next waveform in list */
	};

	private static class WAVEFORM
	{
		EXPRESSION		expression;	/* expression */
		EXPRESSION		time_expr;		/* time expression */
	};

	/********** Names ******************************************************/

	private static final int NONAME				= 0;
	private static final int NAME_SINGLE		= 1;
	private static final int NAME_CONCATENATE	= 2;
	private static final int NAME_ATTRIBUTE		= 3;
	private static class VNAME
	{
		int				type;			/* type of name */
		Object			pointer;		/* pointer to parse tree */
	};

	private static final int NOSINGLENAME			= 0;
	private static final int SINGLENAME_SIMPLE		= 1;
	private static final int SINGLENAME_SELECTED	= 2;
	private static final int SINGLENAME_INDEXED		= 3;
	private static final int SINGLENAME_SLICE		= 4;
	private static class SINGLENAME
	{
		int				type;			/* type of simple name */
		Object			pointer;		/* pointer to parse tree */
	};

	private static class SIMPLENAME
	{
		TOKENLIST		identifier;	/* identifier */
	};

	private static class SELECTEDNAME
	{
		SELECTPREFIX	prefix;		/* prefix */
		SELECTSUFFIX	suffix;		/* suffix */
	};

	private static final int SELECTPREFIX_PREFIX	= 1;
	private static final int SELECTPREFIX_STANDARD	= 2;
	private static class SELECTPREFIX
	{
		int				type;			/* type of prefix */
		Object			pointer;		/* pointer to parse tree */
	};

	private static final int PREFIX_NAME			= 1;
	private static final int PREFIX_FUNCTION_CALL	= 2;
	private static class PREFIX
	{
		int				type;			/* type of prefix */
		Object			pointer;		/* pointer to parse tree */
	};

	private static final int SELECTSUFFIX_SIMPLENAME	= 1;
	private static final int SELECTSUFFIX_CHAR_LIT		= 2;			/* character */
	private static class SELECTSUFFIX
	{
		int				type;			/* type of suffix */
		Object			pointer;		/* pointer to parse tree */
	};

	private static class INDEXEDNAME
	{
		PREFIX			prefix;		/* prefix */
		EXPRLIST		expr_list;		/* expression list */
	};

	private static class EXPRLIST
	{
		EXPRESSION		expression;	/* expression */
		EXPRLIST		next;			/* next in list */
	};

	private static class SLICENAME
	{
		PREFIX			prefix;		/* prefix */
		DISCRETERANGE	range;			/* discrete range */
	};

	private static final int DISCRETERANGE_SUBTYPE	= 1;
	private static final int DISCRETERANGE_RANGE	= 2;
	private static class DISCRETERANGE
	{
		int				type;			/* type of discrete range */
		Object			pointer;		/* pointer to parse tree */
	};

	private static final int RANGE_ATTRIBUTE	= 1;
	private static final int RANGE_SIMPLE_EXPR	= 2;
	private static class RANGE
	{
		int				type;			/* type of range */
		Object			pointer;		/* pointer to parse tree */
	};

	private static class RANGESIMPLE
	{
		SIMPLEEXPR		start;			/* start of range */
		SIMPLEEXPR		end;			/* end of range */
	};

	private static class CONCATENATEDNAME
	{
		SINGLENAME		name;			/* single name */
		CONCATENATEDNAME next;		/* next in list */
	};

	/********** Expressions ************************************************/

	private static final int NOLOGOP		= 0;
	private static final int LOGOP_AND		= 1;
	private static final int LOGOP_OR		= 2;
	private static final int LOGOP_NAND		= 3;
	private static final int LOGOP_NOR		= 4;
	private static final int LOGOP_XOR		= 5;
	private static class EXPRESSION
	{
		RELATION		relation;		/* first relation */
		MRELATIONS		next;			/* more relations */
	};

	private static final int NORELOP		= 0;
	private static final int RELOP_EQ		= 1;
	private static final int RELOP_NE		= 2;
	private static final int RELOP_LT		= 3;
	private static final int RELOP_LE		= 4;
	private static final int RELOP_GT		= 5;
	private static final int RELOP_GE		= 6;
	private static class RELATION
	{
		SIMPLEEXPR		simple_expr;	/* simple expression */
		int				rel_operator;	/* possible operator */
		SIMPLEEXPR		simple_expr2;	/* possible expression */
	};

	private static class MRELATIONS
	{
		int				log_operator;	/* logical operator */
		RELATION		relation;		/* relation */
		MRELATIONS		next;			/* more relations */
	};

	private static final int NOADDOP			= 0;
	private static final int ADDOP_ADD			= 1;
	private static final int ADDOP_SUBTRACT		= 2;
	private static class SIMPLEEXPR
	{
		int				sign;			/* sign (1 or  -1) */
		TERM			term;			/* first term */
		MTERMS			next;			/* additional terms */
	};

	private static final int NOMULOP			= 0;
	private static final int MULOP_MULTIPLY		= 1;
	private static final int MULOP_DIVIDE		= 2;
	private static final int MULOP_MOD			= 3;
	private static final int MULOP_REM			= 4;
	private static class TERM
	{
		FACTOR			factor;		/* first factor */
		MFACTORS		next;			/* additional factors */
	};

	private static class MTERMS
	{
		int				add_operator;	/* add operator */
		TERM			term;			/* next term */
		MTERMS			next;			/* any more terms */
	};

	private static final int NOMISCOP			= 0;
	private static final int MISCOP_POWER		= 1;
	private static final int MISCOP_ABS			= 2;
	private static final int MISCOP_NOT			= 3;
	private static class FACTOR
	{
		PRIMARY			primary;		/* first primary */
		int				misc_operator;	/* possible operator */
		PRIMARY			primary2;		/* possible primary */
	};

	private static class MFACTORS
	{
		int				mul_operator;	/* operator */
		FACTOR			factor;		/* next factor */
		MFACTORS		next;			/* possible more factors */
	};

	private static final int NOPRIMARY					= 0;
	private static final int PRIMARY_NAME				= 1;
	private static final int PRIMARY_LITERAL			= 2;
	private static final int PRIMARY_AGGREGATE			= 3;
	private static final int PRIMARY_CONCATENATION		= 4;
	private static final int PRIMARY_FUNCTION_CALL		= 5;
	private static final int PRIMARY_TYPE_CONVERSION	= 6;
	private static final int PRIMARY_QUALIFIED_EXPR		= 7;
	private static final int PRIMARY_EXPRESSION			= 8;
	private static class PRIMARY
	{
		int				type;			/* type of primary */
		Object			pointer;		/* pointer to primary */
	};

	private static final int NOLITERAL				= 0;
	private static final int LITERAL_NUMERIC		= 1;
	private static final int LITERAL_ENUMERATION	= 2;
	private static final int LITERAL_STRING			= 3;
	private static final int LITERAL_BIT_STRING		= 4;
	private static class LITERAL
	{
		int				type;			/* type of literal */
		Object			pointer;		/* pointer to parse tree */
	};

	/* special codes during VHDL generation */
	private static final int BLOCKNORMAL   =  0;		/* ordinary block */
	private static final int BLOCKMOSTRAN  =  1;		/* a MOS transistor */
	private static final int BLOCKBUFFER   =  2;		/* a buffer */
	private static final int BLOCKPOSLOGIC =  3;		/* an and, or, xor */
	private static final int BLOCKINVERTER =  4;		/* an inverter */
	private static final int BLOCKNAND     =  5;		/* a nand */
	private static final int BLOCKNOR      =  6;		/* a nor */
	private static final int BLOCKXNOR     =  7;		/* an xnor */
	private static final int BLOCKFLOPDS   =  8;		/* a settable D flip-flop */
	private static final int BLOCKFLOPDR   =  9;		/* a resettable D flip-flop */
	private static final int BLOCKFLOPTS   = 10;		/* a settable T flip-flop */
	private static final int BLOCKFLOPTR   = 11;		/* a resettable T flip-flop */
	private static final int BLOCKFLOP     = 12;		/* a general flip-flop */
	
	private static String	vhdl_delimiterstr = "&'()*+,-./:;<=>|";
	private static String	vhdl_doubledelimiterstr = "=>..**:=/=>=<=<>";
	
	private static VKEYWORD [] vhdl_keywords =
	{
		new VKEYWORD("abs",				KEY_ABS),
		new VKEYWORD("after",			KEY_AFTER),
		new VKEYWORD("alias",			KEY_ALIAS),
		new VKEYWORD("all",				KEY_ALL),
		new VKEYWORD("and",				KEY_AND),
		new VKEYWORD("architecture",	KEY_ARCHITECTURE),
		new VKEYWORD("array",			KEY_ARRAY),
		new VKEYWORD("assertion",		KEY_ASSERTION),
		new VKEYWORD("attribute",		KEY_ATTRIBUTE),
		new VKEYWORD("begin",			KEY_BEGIN),
		new VKEYWORD("behavioral",		KEY_BEHAVIORAL),
		new VKEYWORD("body",			KEY_BODY),
		new VKEYWORD("case",			KEY_CASE),
		new VKEYWORD("component",		KEY_COMPONENT),
		new VKEYWORD("connect",			KEY_CONNECT),
		new VKEYWORD("constant",		KEY_CONSTANT),
		new VKEYWORD("convert",			KEY_CONVERT),
		new VKEYWORD("dot",				KEY_DOT),
		new VKEYWORD("downto",			KEY_DOWNTO),
		new VKEYWORD("else",			KEY_ELSE),
		new VKEYWORD("elsif",			KEY_ELSIF),
		new VKEYWORD("end",				KEY_END),
		new VKEYWORD("entity",			KEY_ENTITY),
		new VKEYWORD("exit",			KEY_EXIT),
		new VKEYWORD("for",				KEY_FOR),
		new VKEYWORD("function",		KEY_FUNCTION),
		new VKEYWORD("generate",		KEY_GENERATE),
		new VKEYWORD("generic",			KEY_GENERIC),
		new VKEYWORD("if",				KEY_IF),
		new VKEYWORD("in",				KEY_IN),
		new VKEYWORD("inout",			KEY_INOUT),
		new VKEYWORD("is",				KEY_IS),
		new VKEYWORD("library",			KEY_LIBRARY),
		new VKEYWORD("linkage",			KEY_LINKAGE),
		new VKEYWORD("loop",			KEY_LOOP),
		new VKEYWORD("map",				KEY_MAP),
		new VKEYWORD("mod",				KEY_MOD),
		new VKEYWORD("nand",			KEY_NAND),
		new VKEYWORD("next",			KEY_NEXT),
		new VKEYWORD("nor",				KEY_NOR),
		new VKEYWORD("not",				KEY_NOT),
		new VKEYWORD("null",			KEY_NULL),
		new VKEYWORD("of",				KEY_OF),
		new VKEYWORD("open",			KEY_OPEN),
		new VKEYWORD("or",				KEY_OR),
		new VKEYWORD("others",			KEY_OTHERS),
		new VKEYWORD("out",				KEY_OUT),
		new VKEYWORD("package",			KEY_PACKAGE),
		new VKEYWORD("port",			KEY_PORT),
		new VKEYWORD("range",			KEY_RANGE),
		new VKEYWORD("record",			KEY_RECORD),
		new VKEYWORD("rem",				KEY_REM),
		new VKEYWORD("report",			KEY_REPORT),
		new VKEYWORD("resolve",			KEY_RESOLVE),
		new VKEYWORD("return",			KEY_RETURN),
		new VKEYWORD("severity",		KEY_SEVERITY),
		new VKEYWORD("signal",			KEY_SIGNAL),
		new VKEYWORD("standard",		KEY_STANDARD),
		new VKEYWORD("static",			KEY_STATIC),
		new VKEYWORD("subtype",			KEY_SUBTYPE),
		new VKEYWORD("then",			KEY_THEN),
		new VKEYWORD("to",				KEY_TO),
		new VKEYWORD("type",			KEY_TYPE),
		new VKEYWORD("units",			KEY_UNITS),
		new VKEYWORD("use",				KEY_USE),
		new VKEYWORD("variable",		KEY_VARIABLE),
		new VKEYWORD("when",			KEY_WHEN),
		new VKEYWORD("while",			KEY_WHILE),
		new VKEYWORD("with",			KEY_WITH),
		new VKEYWORD("xor",				KEY_XOR)
	};
	
	private HashSet vhdl_identtable;
	private TOKENLIST  vhdl_tliststart;
	private TOKENLIST  vhdl_tlistend;
	private boolean      vhdl_externentities;		/* enternal entities flag */
	private boolean   vhdl_warnflag;			/* warning flag, TRUE warn */
	private Library    vhdl_lib;				/* behavioral library */
	private int vhdl_errorcount;
	private boolean hasErrors;
	private UNRESLIST  vhdl_unresolved_list;


	/**
	 * The constructor compiles the VHDL and produces a netlist.
	 */
	public CompileVHDL(Cell vhdlCell)
	{
		hasErrors = true;
		String [] strings = vhdlCell.getTextViewContents();
		if (strings == null)
		{
			System.out.println("Cell " + vhdlCell.describe() + " has no text in it");
			return;
		}

		// initialize
		vhdl_externentities = true;
		vhdl_warnflag = false;
		vhdl_lib = null;
		vhdl_unresolved_list = null;

		// build and clear vhdl_identtable
		vhdl_identtable = new HashSet();

		vhdl_errorcount = 0;
		vhdl_scanner(strings);
		if (vhdl_parser(vhdl_tliststart)) return;
		if (vhdl_semantic()) return;
		hasErrors = false;
	}

	/**
	 * Method to report whether the VHDL compile was successful.
	 * @return true if there were errors.
	 */
	public boolean hasErrors() { return hasErrors; };

	public List getQUISCNetlist()
	{
		// now produce the netlist
		if (hasErrors) return null;
		List netlistStrings = vhdl_genquisc();
		return netlistStrings;
	}

	/******************************** THE VHDL SCANNER ********************************/

	/**
	 * Method to do lexical scanning of input VHDL and create token list.
	 */
	private void vhdl_scanner(String [] strings)
	{
		String buf = "";
		int bufPos = 0;
		int line_num = 0;
		boolean space = false;
		for(;;)
		{
			if (bufPos >= buf.length())
			{
				if (line_num >= strings.length) return;
				buf = strings[line_num++];
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
				if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '"')
				{
					char tchar = Character.toUpperCase(c);
					if (tchar == 'B')
					{
						// EMPTY 
					} else if (tchar == '0')
					{
						// EMPTY 
					} else if (tchar == 'X')
					{
						// EMPTY 
					}
				}
				int end = bufPos;
				for(; end < buf.length(); end++)
				{
					char eChar = buf.charAt(end);
					if (!Character.isLetterOrDigit(eChar) && eChar != '_') break;
				}
	
				// got alphanumeric from c to end - 1
				VKEYWORD key = vhdl_iskeyword(buf.substring(bufPos, end));
				if (key != null)
				{
					vhdl_makekeytoken(key, line_num, space);
				} else
				{
					vhdl_makeidenttoken(buf.substring(bufPos, end), line_num, space);
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
				vhdl_makedecimaltoken(buf.substring(bufPos, end), line_num, space);
				bufPos = end;
			} else
			{
				switch (c)
				{
					case '"':
						// got a start of a string
						int end = bufPos + 1;
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
						vhdl_makestrtoken(buf.substring(bufPos + 1, end), line_num, space);
						if (buf.charAt(end) == '"') end++;
						bufPos = end;
						break;
					case '&':
						vhdl_maketoken(TOKEN_AMPERSAND, line_num, space);
						bufPos++;
						break;
					case '\'':
						// character literal
						if (bufPos+2 < buf.length() && buf.charAt(bufPos+2) == '\'')
						{
							vhdl_makechartoken(buf.charAt(bufPos+1), line_num, space);
							bufPos += 3;
						} else
							bufPos++;
						break;
					case '(':
						vhdl_maketoken(TOKEN_LEFTBRACKET, line_num, space);
						bufPos++;
						break;
					case ')':
						vhdl_maketoken(TOKEN_RIGHTBRACKET, line_num, space);
						bufPos++;
						break;
					case '*':
						// could be STAR or DOUBLESTAR
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '*')
						{
							vhdl_maketoken(TOKEN_DOUBLESTAR, line_num, space);
							bufPos += 2;
						} else
						{
							vhdl_maketoken(TOKEN_STAR, line_num, space);
							bufPos++;
						}
						break;
					case '+':
						vhdl_maketoken(TOKEN_PLUS, line_num, space);
						bufPos++;
						break;
					case ',':
						vhdl_maketoken(TOKEN_COMMA, line_num, space);
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
							vhdl_maketoken(TOKEN_MINUS, line_num, space);
							bufPos++;
						}
						break;
					case '.':
						// could be PERIOD or DOUBLEDOT
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '.')
						{
							vhdl_maketoken(TOKEN_DOUBLEDOT, line_num, space);
							bufPos += 2;
						} else
						{
							vhdl_maketoken(TOKEN_PERIOD, line_num, space);
							bufPos++;
						}
						break;
					case '/':
						// could be SLASH or NE
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '=')
						{
							vhdl_maketoken(TOKEN_NE, line_num, space);
							bufPos += 2;
						} else
						{
							vhdl_maketoken(TOKEN_SLASH, line_num, space);
							bufPos++;
						}
						break;
					case ':':
						// could be COLON or VARASSIGN
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '=')
						{
							vhdl_maketoken(TOKEN_VARASSIGN, line_num, space);
							bufPos += 2;
						} else
						{
							vhdl_maketoken(TOKEN_COLON, line_num, space);
							bufPos++;
						}
						break;
					case ';':
						vhdl_maketoken(TOKEN_SEMICOLON, line_num, space);
						bufPos++;
						break;
					case '<':
						// could be LT or LE or BOX
						if (bufPos+1 < buf.length())
						{
							if (buf.charAt(bufPos+1) == '=')
							{
								vhdl_maketoken(TOKEN_LE, line_num, space);
								bufPos += 2;
								break;
							}
							if (buf.charAt(bufPos+1) == '>')
							{
								vhdl_maketoken(TOKEN_BOX, line_num, space);
								bufPos += 2;
								break;
							}
						}
						vhdl_maketoken(TOKEN_LT, line_num, space);
						bufPos++;
						break;
					case '=':
						// could be EQUAL or double delimiter ARROW
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '>')
						{
							vhdl_maketoken(TOKEN_ARROW, line_num, space);
							bufPos += 2;
						} else
						{
							vhdl_maketoken(TOKEN_EQ, line_num, space);
							bufPos++;
						}
						break;
					case '>':
						// could be GT or GE
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '=')
						{
							vhdl_maketoken(TOKEN_GE, line_num, space);
							bufPos += 2;
						} else
						{
							vhdl_maketoken(TOKEN_GT, line_num, space);
							bufPos++;
						}
						break;
					case '|':
						vhdl_maketoken(TOKEN_VERTICALBAR, line_num, space);
						bufPos++;
						break;
					default:
						//	AJ	vhdl_makestrtoken(TOKEN_UNKNOWN, c, 1, line_num, space);
						vhdl_maketoken(TOKEN_UNKNOWN, line_num, space);
						bufPos++;
						break;
				}
			}
		}
	}

	/**
	 * Method to add a token to the token list which has a key reference.
	 * @param key pointer to keyword in table.
	 * @param line_num line number of occurence.
	 * @param space previous space flag.
	 */
	private void vhdl_makekeytoken(VKEYWORD key, int line_num, boolean space)
	{
		TOKENLIST newtoken = new TOKENLIST();
		newtoken.token = TOKEN_KEYWORD;
		newtoken.pointer = key;
		newtoken.line_num = line_num;
		newtoken.space = true;
		newtoken.next = null;
		newtoken.last = vhdl_tlistend;
		if (vhdl_tlistend == null)
		{
			vhdl_tliststart = vhdl_tlistend = newtoken;
		} else
		{
			vhdl_tlistend.space = space;
			vhdl_tlistend.next = newtoken;
			vhdl_tlistend = newtoken;
		}
	}
	
	/**
	 * Method to add a identity token to the token list which has a string reference.
	 * @param newstring start of string.
	 * @param line_num line number of occurence.
	 * @param space previous space flag.
	 */
	private void vhdl_makeidenttoken(String newstring, int line_num, boolean space)
	{
		TOKENLIST newtoken = new TOKENLIST();
		newtoken.token = TOKEN_IDENTIFIER;
	
		// check if ident exits in the global name space
		vhdl_identtable.add(newstring);
		newtoken.pointer = newstring;
		newtoken.line_num = line_num;
		newtoken.space = true;
		newtoken.next = null;
		newtoken.last = vhdl_tlistend;
		if (vhdl_tlistend == null)
		{
			vhdl_tliststart = vhdl_tlistend = newtoken;
		} else
		{
			vhdl_tlistend.space = space;
			vhdl_tlistend.next = newtoken;
			vhdl_tlistend = newtoken;
		}
	}
	
	/**
	 * Method to add a string token to the token list.
	 * Note that two adjacent double quotes should be mergered into one.
	 * @param newstring the string.
	 * @param line_num line number of occurence.
	 * @param space previous space flag.
	 */
	private void vhdl_makestrtoken(String newstring, int line_num, boolean space)
	{
		TOKENLIST newtoken = new TOKENLIST();
		newtoken.token = TOKEN_STRING;
	
		// merge two adjacent double quotes
		newstring.replace("\"\"", "\"");
		newtoken.pointer = newstring;
		newtoken.line_num = line_num;
		newtoken.space = true;
		newtoken.next = null;
		newtoken.last = vhdl_tlistend;
		if (vhdl_tlistend == null)
		{
			vhdl_tliststart = vhdl_tlistend = newtoken;
		} else
		{
			vhdl_tlistend.space = space;
			vhdl_tlistend.next = newtoken;
			vhdl_tlistend = newtoken;
		}
	}
	
	/**
	 * Method to add a numeric token to the token list which has a string reference.
	 * @param newstring the string.
	 * @param line_num line number of occurence.
	 * @param space previous space flag.
	 */
	private void vhdl_makedecimaltoken(String newstring, int line_num, boolean space)
	{
		TOKENLIST newtoken = new TOKENLIST();
		newtoken.token = TOKEN_DECIMAL;
		newtoken.pointer = newstring;
		newtoken.line_num = line_num;
		newtoken.space = true;
		newtoken.next = null;
		newtoken.last = vhdl_tlistend;
		if (vhdl_tlistend == null)
		{
			vhdl_tliststart = vhdl_tlistend = newtoken;
		} else
		{
			vhdl_tlistend.space = space;
			vhdl_tlistend.next = newtoken;
			vhdl_tlistend = newtoken;
		}
	}
	
	/**
	 * Method to add a token to the token list which has no string reference.
	 * @param token token number.
	 * @param line_num line number of occurence.
	 * @param space previous space flag.
	 */
	private void vhdl_maketoken(int token, int line_num, boolean space)
	{
		TOKENLIST newtoken = new TOKENLIST();
		newtoken.token = token;
		newtoken.pointer = null;
		newtoken.line_num = line_num;
		newtoken.space = true;
		newtoken.next = null;
		newtoken.last = vhdl_tlistend;
		if (vhdl_tlistend == null)
		{
			vhdl_tliststart = vhdl_tlistend = newtoken;
		} else
		{
			vhdl_tlistend.space = space;
			vhdl_tlistend.next = newtoken;
			vhdl_tlistend = newtoken;
		}
	}
	
	/**
	 * Method to make a character literal token.
	 * @param c character literal.
	 * @param line_num number of source line.
	 * @param space previous space flag.
	 */
	private void vhdl_makechartoken(char c, int line_num, boolean space)
	{
		TOKENLIST newtoken = new TOKENLIST();
		newtoken.token = TOKEN_CHAR;
		newtoken.pointer = new Character(c);
		newtoken.line_num = line_num;
		newtoken.space = true;
		newtoken.next = null;
		newtoken.last = vhdl_tlistend;
		if (vhdl_tlistend == null)
		{
			vhdl_tliststart = vhdl_tlistend = newtoken;
		} else
		{
			vhdl_tlistend.space = space;
			vhdl_tlistend.next = newtoken;
			vhdl_tlistend = newtoken;
		}
	}

	/**
	 * Method to get address in the keyword table.
	 * @param tstring string to lookup.
	 * @return entry in keywords table if keyword, else null.
	 */
	public static VKEYWORD vhdl_iskeyword(String tstring)
	{
		int base = 0;
		int num = vhdl_keywords.length;
		int aindex = num >> 1;
		while (num != 0)
		{
			int check = tstring.compareTo(vhdl_keywords[base + aindex].name);
			if (check == 0) return vhdl_keywords[base + aindex];
			if (check < 0)
			{
				num = aindex;
				aindex = num >> 1;
			} else
			{
				base += aindex + 1;
				num -= aindex + 1;
				aindex = num >> 1;
			}
		}
		return null;
	}

	/******************************** THE VHDL PARSER ********************************/

	private boolean			vhdl_err;
	private TOKENLIST		vhdl_nexttoken;
	private PTREE			vhdl_ptree;

	private class ParseException extends Exception
	{
	}

	/**
	 * Method to parse the passed token list using the parse tables.
	 * Reports on any syntax errors and create the required syntax trees.
	 * @param tlist list of tokens.
	 */
	private boolean vhdl_parser(TOKENLIST tlist)
	{
		vhdl_err = false;
		vhdl_ptree = null;
		PTREE endunit = null;
		vhdl_nexttoken = tlist;
		try
		{
			while (vhdl_nexttoken != null)
			{
				if (vhdl_nexttoken.token == TOKEN_KEYWORD)
				{
					int type = NOUNIT;
					VKEYWORD vk = (VKEYWORD)vhdl_nexttoken.pointer;
					Object pointer = null;
					switch (vk.num)
					{
						case KEY_LIBRARY:
							vhdl_parsetosemicolon();
							break;
						case KEY_ENTITY:
							type = UNIT_INTERFACE;
							pointer = vhdl_parseinterface();
							break;
						case KEY_ARCHITECTURE:
							type = UNIT_BODY;
							pointer = vhdl_parsebody(BODY_ARCHITECTURAL);
							break;
						case KEY_PACKAGE:
							type = UNIT_PACKAGE;
							pointer = vhdl_parsepackage();
							break;
						case KEY_USE:
							type = UNIT_USE;
							pointer = vhdl_parseuse();
							break;
						default:
							vhdl_reporterrormsg(vhdl_nexttoken, "No entry keyword - entity, architectural, behavioral");
							vhdl_nexttoken = vhdl_nexttoken.next;
							break;
					}
					if (type != NOUNIT)
					{
						PTREE newunit = new PTREE();
						newunit.type = type;
						newunit.pointer = pointer;
						newunit.next = null;
						if (endunit == null)
						{
							vhdl_ptree = endunit = newunit;
						} else
						{
							endunit.next = newunit;
							endunit = newunit;
						}
					}
				} else
				{
					vhdl_reporterrormsg(vhdl_nexttoken, "No entry keyword - entity, architectural, behavioral");
					vhdl_nexttoken = vhdl_nexttoken.next;
				}
			}
		} catch (ParseException e)
		{
		}
		return vhdl_err;
	}
	
	/**
	 * Method to parse an interface description of the form:
	 *    ENTITY identifier IS PORT (formal_port_list);
	 *    END [identifier] ;
	 */
	private VINTERFACE vhdl_parseinterface()
		throws ParseException
	{
		vhdl_getnexttoken();
	
		// check for entity IDENTIFIER
		TOKENLIST name = null;
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
		} else
		{
			name = vhdl_nexttoken;
		}
	
		// check for keyword IS
		vhdl_getnexttoken();
		if (!vhdl_keysame(vhdl_nexttoken, KEY_IS))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword IS");
		}
	
		// check for keyword PORT
		vhdl_getnexttoken();
		if (!vhdl_keysame(vhdl_nexttoken, KEY_PORT))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword PORT");
		}
	
		// check for opening bracket of FORMAL_PORT_LIST
		vhdl_getnexttoken();
		if (vhdl_nexttoken.token != TOKEN_LEFTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a left bracket");
		}
	
		// gather FORMAL_PORT_LIST
		vhdl_getnexttoken();
		FPORTLIST ports = vhdl_parseformal_port_list();
		if (ports == null)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Interface must have ports");
		}
	
		// check for closing bracket of FORMAL_PORT_LIST
		if (vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a right bracket");
		}
	
		vhdl_getnexttoken();
		// check for SEMICOLON
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		else vhdl_getnexttoken();
	
		// check for keyword END
		if (!vhdl_keysame(vhdl_nexttoken, KEY_END))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword END");
		}
	
		// check for optional entity IDENTIFIER
		vhdl_getnexttoken();
		if (vhdl_nexttoken.token == TOKEN_IDENTIFIER)
		{
			if (!vhdl_nexttoken.pointer.equals(name.pointer))
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Unmatched entity identifier names");
			}
			vhdl_getnexttoken();
		}
	
		// check for closing SEMICOLON
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_nexttoken = vhdl_nexttoken.next;
	
		// allocate an entity parse tree
		VINTERFACE interfacef = new VINTERFACE();
		interfacef.name = name;
		interfacef.ports = ports;
		interfacef.interfacef = null;
		return interfacef;
	}

	/**
	 * Method to parse a body.  The syntax is of the form:
	 *    ARCHITECTURE identifier OF simple_name IS
	 *      body_declaration_part
	 *    BEGIN
	 *      set_of_statements
	 *    END [identifier] ;
	 * @param vclass body class (ARCHITECTURAL or BEHAVIORAL).
	 * @return the created body structure.
	 */
	private BODY vhdl_parsebody(int vclass)
		throws ParseException
	{
		vhdl_getnexttoken();
	
		// next is body_name (identifier)
		TOKENLIST body_name = null;
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
		} else
		{
			body_name = vhdl_nexttoken;
		}
		vhdl_getnexttoken();
	
		// check for keyword OF
		if (!vhdl_keysame(vhdl_nexttoken, KEY_OF))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword OF");
		}
		vhdl_getnexttoken();
	
		// next is design entity reference for this body (simple_name)
		SIMPLENAME entity_name = vhdl_parsesimplename();
		if (entity_name == null)
		{
			// EMPTY 
		}
	
		// check for keyword IS
		if (!vhdl_keysame(vhdl_nexttoken, KEY_IS))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword IS");
		}
		vhdl_getnexttoken();
	
		// body declaration part
		BODYDECLARE body_declare = vhdl_parsebody_declare();
		if (body_declare == null)
		{
			// EMPTY 
		}
	
		// should be at keyword BEGIN
		if (!vhdl_keysame(vhdl_nexttoken, KEY_BEGIN))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword BEGIN");
		}
		vhdl_getnexttoken();
	
		// statements of body
		STATEMENTS statements = vhdl_parseset_of_statements();
		if (statements == null)
		{
			// EMPTY 
		}
	
		// should be at keyword END
		if (!vhdl_keysame(vhdl_nexttoken, KEY_END))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword END");
		}
		vhdl_getnexttoken();
	
		// optional body name
		if (vhdl_nexttoken.token == TOKEN_IDENTIFIER)
		{
			if (!vhdl_nexttoken.pointer.equals(body_name.pointer))
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Body name mismatch");
			}
			vhdl_getnexttoken();
		}
	
		// should be at final semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_nexttoken = vhdl_nexttoken.next;
	
		// create body parse tree
		BODY body = new BODY();
		body.classnew = vclass;
		body.name = body_name;
		body.entity = entity_name;
		body.body_declare = body_declare;
		body.statements = statements;
		return body;
	}
	
	/**
	 * Method to parse a package declaration.
	 * It has the form:
	 *    package_declaration ::= PACKAGE identifier IS
	 *      package_declarative_part
	 *    END [simple_name] ;
	 * @return the package declaration.
	 */
	private PACKAGE vhdl_parsepackage()
		throws ParseException
	{
		PACKAGE vpackage = null;
	
		// should be at keyword package
		if (!vhdl_keysame(vhdl_nexttoken, KEY_PACKAGE))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword PACKAGE");
			vhdl_getnexttoken();
			return vpackage;
		}
		vhdl_getnexttoken();
	
		// should be package identifier
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
			vhdl_getnexttoken();
			return vpackage;
		}
		TOKENLIST identifier = vhdl_nexttoken;
		vhdl_getnexttoken();
	
		// should be at keyword IS
		if (!vhdl_keysame(vhdl_nexttoken, KEY_IS))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword IS");
			vhdl_getnexttoken();
			return vpackage;
		}
		vhdl_getnexttoken();
	
		// package declarative part
		PACKAGEDPART declare_part = vhdl_parsepackage_declare_part();
	
		// should be at keyword END
		if (!vhdl_keysame(vhdl_nexttoken, KEY_END))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword END");
			vhdl_getnexttoken();
			return vpackage;
		}
		vhdl_getnexttoken();
	
		// check for optional end identifier
		if (vhdl_nexttoken.token == TOKEN_IDENTIFIER)
		{
			if (!vhdl_nexttoken.pointer.equals(identifier.pointer))
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Name mismatch");
				vhdl_getnexttoken();
				return vpackage;
			}
			vhdl_getnexttoken();
		}
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
			vhdl_getnexttoken();
			return vpackage;
		}
		vhdl_getnexttoken();
	
		// create package structure
		vpackage = new PACKAGE();
		vpackage.name = identifier;
		vpackage.declare = declare_part;
	
		return vpackage;
	}
	
	/**
	 * Method to parse a use clause.
	 * It has the form:
	 *    use_clause ::= USE unit {,unit} ;
	 *    unit ::= package_name.ALL
	 * @return the use clause structure.
	 */
	private USE vhdl_parseuse()
		throws ParseException
	{
		USE use = null;
	
		// should be at keyword USE
		if (!vhdl_keysame(vhdl_nexttoken, KEY_USE))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword USE");
			vhdl_getnexttoken();
			return use;
		}
		vhdl_getnexttoken();
	
		// must be at least one unit
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Bad unit name for use clause");
			vhdl_getnexttoken();
			return use;
		}
		use = new USE();
		use.unit = vhdl_nexttoken;
		use.next = null;
		USE enduse = use;
		vhdl_getnexttoken();

		// IEEE version uses form unit.ALL only
		for(;;)
		{
			if (vhdl_nexttoken.token != TOKEN_PERIOD)
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Expecting period");
				break;
			}
			vhdl_getnexttoken();
	
			if (vhdl_keysame(vhdl_nexttoken, KEY_ALL))
			{
				vhdl_getnexttoken();
				break;
			}
			if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Bad unit name for use clause");
				break;
			}
			vhdl_getnexttoken();
		}
	
		while (vhdl_nexttoken.token == TOKEN_COMMA)
		{
			vhdl_getnexttoken();
			if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Bad unit name for use clause");
				vhdl_getnexttoken();
				return use;
			}
			USE newuse = new USE();
			newuse.unit = vhdl_nexttoken;
			newuse.next = null;
			enduse.next = newuse;
			enduse = newuse;
			vhdl_getnexttoken();

			// IEEE version uses form unit.ALL only
			if (vhdl_nexttoken.token == TOKEN_PERIOD)
				vhdl_getnexttoken();
			else vhdl_reporterrormsg(vhdl_nexttoken, "Expecting period");
	
			if (vhdl_keysame(vhdl_nexttoken, KEY_ALL))
				vhdl_getnexttoken();
			else vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword ALL");
		}
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_getnexttoken();
	
		return use;
	}

	/**
	 * Method to parse a package declarative part.
	 * It has the form:
	 *    package_declarative_part ::= package_declarative_item {package_declarative_item}
	 *    package_declarative_item ::= basic_declaration | function_declaration
	 * Note:  Currently only support basic declarations.
	 * @return the package declarative part.
	 */
	private PACKAGEDPART vhdl_parsepackage_declare_part()
		throws ParseException
	{
		PACKAGEDPART dpart = null;
	
		// should be at least one
		if (vhdl_keysame(vhdl_nexttoken, KEY_END))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "No Package declarative part");
			return dpart;
		}
		BASICDECLARE ditem = vhdl_parsebasic_declare();
		dpart = new PACKAGEDPART();
		dpart.item = ditem;
		dpart.next = null;
		PACKAGEDPART endpart = dpart;
	
		while (!vhdl_keysame(vhdl_nexttoken, KEY_END))
		{
			ditem = vhdl_parsebasic_declare();
			PACKAGEDPART newpart = new PACKAGEDPART();
			newpart.item = ditem;
			newpart.next = null;
			endpart.next = newpart;
			endpart = newpart;
		}
	
		return dpart;
	}

	/**
	 * Method to parse the body statements and return pointer to the parse tree.
	 * The form of body statements are:
	 *    set_of_statements :== architectural_statement {architectural_statement}
	 *    architectural_statement :== generate_statement | signal_assignment_statement | architectural_if_statement | architectural_case_statement | component_instantiation_statement | null_statement
	 * @return the statements parse tree.
	 */
	private STATEMENTS vhdl_parseset_of_statements()
		throws ParseException
	{
		STATEMENTS statements = null;
		STATEMENTS endstate = null;
		while (!vhdl_keysame(vhdl_nexttoken, KEY_END))
		{
			int type = NOARCHSTATE;
			Object pointer = null;
	
			// check for case statement
			if (vhdl_keysame(vhdl_nexttoken, KEY_CASE))
			{
				// EMPTY 
			}
	
			// check for null statement
			else if (vhdl_keysame(vhdl_nexttoken, KEY_NULL))
			{
				type = ARCHSTATE_NULL;
				pointer = null;
				vhdl_getnexttoken();
				// should be a semicolon
				if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
				{
					vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
				}
				vhdl_getnexttoken();
			}
	
			// check for label
			else if (vhdl_nexttoken.token == TOKEN_IDENTIFIER && vhdl_nexttoken.next != null &&
				vhdl_nexttoken.next.token == TOKEN_COLON)
			{
				TOKENLIST label = vhdl_nexttoken;
				vhdl_getnexttoken();
				vhdl_getnexttoken();
				// check for generate statement
				if (vhdl_keysame(vhdl_nexttoken, KEY_IF))
				{
					type = ARCHSTATE_GENERATE;
					pointer = vhdl_parsegenerate(label, GENSCHEME_IF);
				}
				else if (vhdl_keysame(vhdl_nexttoken, KEY_FOR))
				{
					type = ARCHSTATE_GENERATE;
					pointer = vhdl_parsegenerate(label, GENSCHEME_FOR);
				}
				// should be component_instantiation_declaration
				else
				{
					vhdl_nexttoken = label;
					type = ARCHSTATE_INSTANCE;
					pointer = vhdl_parseinstance();
				}
			}
	
			// should have signal assignment
			else
			{
				// EMPTY 
			}
	
			// add statement if found
			if (type != NOARCHSTATE)
			{
				STATEMENTS newstate = new STATEMENTS();
				newstate.type = type;
				newstate.pointer = pointer;
				newstate.next = null;
				if (endstate == null)
				{
					statements = endstate = newstate;
				} else
				{
					endstate.next = newstate;
					endstate = newstate;
				}
			} else
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Invalid ARCHITECTURAL statement");
				vhdl_nexttoken = vhdl_nexttoken.next;
				break;
			}
		}
	
		return statements;
	}

	/**
	 * Method to parse a component instantiation statement.
	 * It has the form:
	 *    component_instantiation_statement :== label : simple_name PORT MAP(actual_port_list);
	 * @return the instance parse tree.
	 */
	private VINSTANCE vhdl_parseinstance()
		throws ParseException
	{
		VINSTANCE inst = null;
	
		// check for identifier
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
			vhdl_getnexttoken();
			return inst;
		}
		TOKENLIST name = vhdl_nexttoken;
		vhdl_getnexttoken();
	
		// if colon, previous token was the label
		if (vhdl_nexttoken.token == TOKEN_COLON)
		{
			vhdl_getnexttoken();
		} else
		{
			vhdl_nexttoken = name;
			name = null;
		}
	
		// should be at component reference
		SIMPLENAME entity = vhdl_parsesimplename();
	
		// Require PORT MAP
		if (vhdl_keysame(vhdl_nexttoken, KEY_PORT))
		   vhdl_getnexttoken();
		else vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword PORT");
	
		if (vhdl_keysame(vhdl_nexttoken, KEY_MAP))
		   vhdl_getnexttoken();
		else vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword MAP");
	
		// should be at left bracket
		if (vhdl_nexttoken.token != TOKEN_LEFTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a left bracket");
		}
		vhdl_getnexttoken();
		APORTLIST ports = vhdl_parseactual_port_list();
	
		// should be at right bracket
		if (vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a right bracket");
		}
		vhdl_getnexttoken();
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_getnexttoken();
	
		inst = new VINSTANCE();
		inst.name = name;
		inst.entity = entity;
		inst.ports = ports;
		return inst;
	}

	/**
	 * Method to parse an actual port list.
	 * It has the form:
	 *    actual_port_list ::= port_association {, port_association}
	 *    port_association ::= name | OPEN
	 * @return the actual port list structure.
	 */
	private APORTLIST vhdl_parseactual_port_list()
		throws ParseException
	{
		APORTLIST lastport = null;
	
		// should be at least one port association
		APORTLIST aplist = new APORTLIST();
		aplist.type = APORTLIST_NAME;
		if (vhdl_nexttoken.token != TOKEN_COMMA &&
			vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
		{
			if (vhdl_keysame(vhdl_nexttoken, KEY_OPEN))
			{
				aplist.pointer = null;
				vhdl_getnexttoken();
			} else
			{
				aplist.pointer = vhdl_parsename();
				if (vhdl_nexttoken.token == TOKEN_ARROW)
				{
					vhdl_getnexttoken();
					aplist.pointer = vhdl_parsename();
				}
			}
	
		}
		else vhdl_reporterrormsg(vhdl_nexttoken, "No identifier in port list");
	
		aplist.next = null;
		lastport = aplist;
		while (vhdl_nexttoken.token == TOKEN_COMMA)
		{
			vhdl_getnexttoken();
			APORTLIST newport = new APORTLIST();
			newport.type = APORTLIST_NAME;
			if (vhdl_nexttoken.token != TOKEN_COMMA &&
				vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
			{
				if (vhdl_keysame(vhdl_nexttoken, KEY_OPEN))
				{
					newport.pointer = null;
					vhdl_getnexttoken();
				} else
				{
					newport.pointer = vhdl_parsename();
					if (vhdl_nexttoken.token == TOKEN_ARROW)
					{
						vhdl_getnexttoken();
						newport.pointer = vhdl_parsename();
					}
				}
			}
			else vhdl_reporterrormsg(vhdl_nexttoken, "No identifier in port list");
	
			newport.next = null;
			lastport.next = newport;
			lastport = newport;
		}
		return aplist;
	}

	/**
	 * Method to parse a generate statement.
	 * It has the form:
	 *    generate_statement ::= label: generate_scheme GENERATE set_of_statements END GENERATE [label];
	 *    generate_scheme ::= FOR generate_parameter_specification | IF condition
	 *    generate_parameter_specification ::= identifier IN discrete_range
	 * @param label pointer to optional label.
	 * @param gscheme generate scheme (FOR or IF).
	 * @return generate statement structure.
	 */
	private GENERATE vhdl_parsegenerate(TOKENLIST label, int gscheme)
		throws ParseException
	{
		GENERATE gen = null;
	
		if (gscheme == GENSCHEME_FOR)
		{
			// should be past label and at keyword FOR
			if (!vhdl_keysame(vhdl_nexttoken, KEY_FOR))
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword FOR");
			}
		} else
		{
			// should be past label and at keyword IF
			if (!vhdl_keysame(vhdl_nexttoken, KEY_IF))
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword IF");
			}
		}
		GENSCHEME scheme = new GENSCHEME();
		if (gscheme == GENSCHEME_FOR)
		{
			scheme.scheme = GENSCHEME_FOR;
		} else
		{
			scheme.scheme = GENSCHEME_IF;
		}
		scheme.identifier = null;
		scheme.range = null;
		scheme.condition = null;		// for IF scheme only
		vhdl_getnexttoken();
	
		if (gscheme == GENSCHEME_FOR)
		{
			// should be generate parameter specification
			if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
			} else
			{
				scheme.identifier = vhdl_nexttoken;
			}
			vhdl_getnexttoken();
	
			// should be keyword IN
			if (!vhdl_keysame(vhdl_nexttoken, KEY_IN))
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword IN");
			}
			vhdl_getnexttoken();
	
			// should be discrete range
			scheme.range = vhdl_parsediscrete_range();
		} else
		{
			scheme.condition = vhdl_parseexpression();
		}
	
		// should be keyword GENERATE
		if (!vhdl_keysame(vhdl_nexttoken, KEY_GENERATE))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword GENERATE");
		}
		vhdl_getnexttoken();
	
		// set of statements
		STATEMENTS states = vhdl_parseset_of_statements();
	
		// should be at keyword END
		if (!vhdl_keysame(vhdl_nexttoken, KEY_END))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword END");
		}
		vhdl_getnexttoken();
	
		// should be at keyword GENERATE
		if (!vhdl_keysame(vhdl_nexttoken, KEY_GENERATE))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword GENERATE");
		}
		vhdl_getnexttoken();
	
		// check if label should be present
		if (label != null)
		{
			/* For correct IEEE syntax, label is always true, but trailing
			 * label is optional.
			 */
			if (vhdl_nexttoken.token == TOKEN_IDENTIFIER)
			{
				if (!label.pointer.equals(vhdl_nexttoken.pointer))
					vhdl_reporterrormsg(vhdl_nexttoken, "Label mismatch");
				vhdl_getnexttoken();
			}
		}
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_getnexttoken();
	
		// create generate statement structure
		gen = new GENERATE();
		gen.label = label;
		gen.gen_scheme = scheme;
		gen.statements = states;
		return gen;
	}
	
	/**
	 * Method to parse the body declaration and return pointer to the parse tree.
	 * The format is:
	 *    body_declaration_part :== {body_declaration_item}
	 *    body_delaration_item :== basic_declaration | component_declaration | resolution_mechanism_declaration | local_function_declaration
	 * @return the parse tree, null if parsing error encountered.
	 */
	private BODYDECLARE vhdl_parsebody_declare()
		throws ParseException
	{
		BODYDECLARE body =null;
		BODYDECLARE endbody = null;
		Object pointer = null;
		int type = 0;
		while (!vhdl_keysame(vhdl_nexttoken, KEY_BEGIN))
		{
			// check for component declaration
			if (vhdl_keysame(vhdl_nexttoken, KEY_COMPONENT))
			{
				type = BODYDECLARE_COMPONENT;
				pointer = vhdl_parsecomponent();
				if (pointer == null)
				{
					// EMPTY 
				}
			}
			// check for resolution declaration
			else if (vhdl_keysame(vhdl_nexttoken, KEY_RESOLVE))
			{
				type = BODYDECLARE_RESOLUTION;
				pointer = null;
				vhdl_getnexttoken();
			}
			// check for local function declaration
			else if (vhdl_keysame(vhdl_nexttoken, KEY_FUNCTION))
			{
				type = BODYDECLARE_LOCAL;
				pointer = null;
				vhdl_getnexttoken();
			}
			// should be basic declaration
			else
			{
				type = BODYDECLARE_BASIC;
				pointer = vhdl_parsebasic_declare();
				if (pointer == null)
				{
					// EMPTY 
				}
			}
			BODYDECLARE newbody = new BODYDECLARE();
			newbody.type = type;
			newbody.pointer = pointer;
			newbody.next = null;
			if (endbody == null)
			{
				body = endbody = newbody;
			} else
			{
				endbody.next = newbody;
				endbody = newbody;
			}
		}
		return body;
	}

	/**
	 * Method to parse a basic declaration and return a pointer to the parse tree.
	 * The form of a basic declaration is:
	 * basic_declaration :== object_declaration | type_declaration | subtype_declaration | conversion_declaration | attribute_declaration | attribute_specification
	 * @return pointer to basic_declaration parse tree, null if unrecoverable parsing error.
	 */
	private BASICDECLARE vhdl_parsebasic_declare()
		throws ParseException
	{
		BASICDECLARE basic = null;
		int type = NOBASICDECLARE;
		Object pointer = null;
		if (vhdl_keysame(vhdl_nexttoken, KEY_TYPE))
		{
			type = BASICDECLARE_TYPE;
			pointer = vhdl_parsetype();
		}
		else if (vhdl_keysame(vhdl_nexttoken, KEY_SUBTYPE))
		{
			// EMPTY 
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_CONVERT))
		{
			// EMPTY 
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_ATTRIBUTE))
		{
			// EMPTY 
		} else if (vhdl_nexttoken.token == TOKEN_IDENTIFIER)
		{
			// EMPTY 
		} else
		{
			type = BASICDECLARE_OBJECT;
			pointer = vhdl_parseobject_declare();
		}
		if (type != NOBASICDECLARE)
		{
			basic = new BASICDECLARE();
			basic.type = type;
			basic.pointer = pointer;
		} else vhdl_getnexttoken();	// Bug fix , D.J.Yurach, June, 1988
		return basic;
	}

	/**
	 * Method to parse a type declaration.
	 * It has the form: type_declaration ::= TYPE identifier IS type_definition ;
	 * @return the type declaration structure.
	 */
	private TYPE vhdl_parsetype()
		throws ParseException
	{
		TYPE type = null;
	
		// should be at keyword TYPE
		if (!vhdl_keysame(vhdl_nexttoken, KEY_TYPE))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword TYPE");
			vhdl_getnexttoken();
			return type;
		}
		vhdl_getnexttoken();
	
		// should be at type identifier
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
			vhdl_getnexttoken();
			return type;
		}
		TOKENLIST ident = vhdl_nexttoken;
		vhdl_getnexttoken();
	
		// should be keyword IS
		if (!vhdl_keysame(vhdl_nexttoken, KEY_IS))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword IS");
			vhdl_getnexttoken();
			return type;
		}
		vhdl_getnexttoken();
	
		// parse type definition
		Object pointer = null;
		int type_define = 0;
		if (vhdl_keysame(vhdl_nexttoken, KEY_ARRAY))
		{
			type_define = TYPE_COMPOSITE;
			pointer = vhdl_parsecomposite_type();
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_RECORD))
		{
			type_define = TYPE_COMPOSITE;
			pointer = vhdl_parsecomposite_type();
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_RANGE))
		{
			type_define = TYPE_SCALAR;
			pointer = vhdl_parsescalar_type();
		} else if (vhdl_nexttoken.token == TOKEN_LEFTBRACKET)
		{
			type_define = TYPE_SCALAR;
			pointer = vhdl_parsescalar_type();
		} else
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Invalid type definition");
			vhdl_getnexttoken();
			return type;
		}
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
			vhdl_getnexttoken();
			return type;
		}
		vhdl_getnexttoken();
	
		type = new TYPE();
		type.identifier = ident;
		type.type = type_define;
		type.pointer = pointer;
	
		return type;
	}

	private Object vhdl_parsescalar_type() { return null; }

	/**
	 * Method to parse a composite type definition.
	 * It has the form:
	 *    composite_type_definition ::= array_type_definition | record_type_definition
	 */
	private COMPOSITE vhdl_parsecomposite_type()
		throws ParseException
	{
		COMPOSITE compo = null;
	
		// should be ARRAY or RECORD keyword
		Object pointer = null;
		int type = 0;
		if (vhdl_keysame(vhdl_nexttoken, KEY_ARRAY))
		{
			type = COMPOSITE_ARRAY;
			pointer = vhdl_parsearray_type();
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_RECORD))
		{
			type = COMPOSITE_RECORD;
			pointer = vhdl_parserecord_type();
		} else
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Invalid composite type");
			vhdl_getnexttoken();
			return compo;
		}
	
		compo = new COMPOSITE();
		compo.type = type;
		compo.pointer = pointer;
	
		return compo;
	}

	private Object vhdl_parserecord_type() { return null; }

	/**
	 * Method to parse an array type definition.
	 * It has the form:
	 *    array_type_definition ::= unconstrained_array_definition | constrained_array_definition
	 *    unconstrained_array_definition ::= ARRAY (index_subtype_definition {, index_subtype_definition}) OF subtype_indication
	 *    constrained_array_definition ::= ARRAY index_constraint OF subtype_indication
	 *    index_constraint ::= (discrete_range {, discrete_range})
	 * NOTE:  Only currently supporting constrained array definitions.
	 * @return the array type definition.
	 */
	private ARRAY vhdl_parsearray_type()
		throws ParseException
	{
		ARRAY array = null;
	
		// should be keyword ARRAY
		if (!vhdl_keysame(vhdl_nexttoken, KEY_ARRAY))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword ARRAY");
			vhdl_getnexttoken();
			return array;
		}
		vhdl_getnexttoken();
	
		// index_constraint
		// should be left bracket
		if (vhdl_nexttoken.token != TOKEN_LEFTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a left bracket");
			vhdl_getnexttoken();
			return array;
		}
		vhdl_getnexttoken();
	
		// should at least one discrete range
		INDEXCONSTRAINT iconstraint = new INDEXCONSTRAINT();
		iconstraint.discrete = vhdl_parsediscrete_range();
		iconstraint.next = null;
		INDEXCONSTRAINT endconstraint = iconstraint;
	
		// continue while comma
		while (vhdl_nexttoken.token == TOKEN_COMMA)
		{
			vhdl_getnexttoken();
			INDEXCONSTRAINT newconstraint = new INDEXCONSTRAINT();
			newconstraint.discrete = vhdl_parsediscrete_range();
			newconstraint.next = null;
			endconstraint.next = newconstraint;
			endconstraint = newconstraint;
		}
	
		// should be at right bracket
		if (vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a right bracket");
			vhdl_getnexttoken();
			return array;
		}
		vhdl_getnexttoken();
	
		// should be at keyword OF
		if (!vhdl_keysame(vhdl_nexttoken, KEY_OF))
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword OF");
			vhdl_getnexttoken();
			return array;
		}
		vhdl_getnexttoken();
	
		// subtype_indication
		SUBTYPEIND subtype = vhdl_parsesubtype_indication();
	
		// create array type definition
		array = new ARRAY();
		array.type = ARRAY_CONSTRAINED;
		CONSTRAINED constr = new CONSTRAINED();
		array.pointer = constr;
		constr.constraint = iconstraint;
		constr.subtype = subtype;
	
		return array;
	}
	
	/**
	 * Method to parse a discrete range.
	 * The range has the form: discrete_range ::= subtype_indication | range
	 * @return the discrete range structure.
	 */
	private DISCRETERANGE vhdl_parsediscrete_range()
		throws ParseException
	{
		DISCRETERANGE drange = new DISCRETERANGE();

		// currently only support range option
		drange.type = DISCRETERANGE_RANGE;
		drange.pointer = vhdl_parserange();
		return drange;
	}
	
	/**
	 * Method to parse a range.
	 * The range has the form:
	 *    range :== simple_expression direction simple_expression
	 *    direction ::=  TO  |  DOWNTO
	 * @return the range structure.
	 */
	private RANGE vhdl_parserange()
		throws ParseException
	{
		RANGE range = new RANGE();

		// currently support only simple expression range option
		range.type = RANGE_SIMPLE_EXPR;
		range.pointer = vhdl_parserange_simple();
		return range;
	}
	
	/**
	 * Method to parse a simple expression range.
	 * The range has the form: simple_expression .. simple_expression
	 * @return the simple expression range.
	 */
	private RANGESIMPLE vhdl_parserange_simple()
		throws ParseException
	{
		RANGESIMPLE srange = new RANGESIMPLE();
		srange.start = vhdl_parsesimpleexpression();
	
		// Need keyword TO or DOWNTO
		if (vhdl_keysame(vhdl_nexttoken, KEY_TO) || vhdl_keysame(vhdl_nexttoken, KEY_DOWNTO))
		   vhdl_getnexttoken();
		else
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword TO or DOWNTO");
			vhdl_getnexttoken(); // absorb the token anyway (probably "..")
		}
	
		srange.end = vhdl_parsesimpleexpression();
		return srange;
	}

	/**
	 * Method to parse an object declaration and return the pointer to its parse tree.
	 * An object declaration has the form:
	 *    object_declaration :== constant_declaration | signal_declaration | variable_declaration | alias_declaration
	 * @return the object declaration parse tree.
	 */
	private OBJECTDECLARE vhdl_parseobject_declare()
		throws ParseException
	{
		OBJECTDECLARE object = null;
		int type = NOOBJECTDECLARE;
		Object pointer = null;
		if (vhdl_keysame(vhdl_nexttoken, KEY_CONSTANT))
		{
			type = OBJECTDECLARE_CONSTANT;
			pointer = vhdl_parseconstant_declare();
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_SIGNAL))
		{
			type = OBJECTDECLARE_SIGNAL;
			pointer = vhdl_parsesignal_declare();
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_VARIABLE))
		{
			// EMPTY 
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_ALIAS))
		{
			// EMPTY 
		} else
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Invalid object declaration");
		}
		if (type != NOOBJECTDECLARE)
		{
			object = new OBJECTDECLARE();
			object.type = type;
			object.pointer = pointer;
		} else
		{
			vhdl_getnexttoken();
		}
		return object;
	}
	
	/**
	 * Method to parse a constant declaration and return the pointer to the parse tree.
	 * The form of a constant declaration is:
	 *    constant_declaration :== CONSTANT identifier : subtype_indication := expression ;
	 * @return the constant declaration parse tree.
	 */
	private CONSTANTDECLARE vhdl_parseconstant_declare()
		throws ParseException
	{
		CONSTANTDECLARE constant = null;
		vhdl_getnexttoken();
	
		// parse identifier 
		// Note that the standard allows identifier_list here, but we don't support it!
		TOKENLIST ident = null;
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
		} else
		{
			ident = vhdl_nexttoken;
		}
		vhdl_getnexttoken();
	
		// should be at colon
		if (vhdl_nexttoken.token != TOKEN_COLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a colon");
		}
		vhdl_getnexttoken();
	
		// parse subtype indication
		SUBTYPEIND ind = vhdl_parsesubtype_indication();
	
		// should be at assignment symbol
		if (vhdl_nexttoken.token != TOKEN_VARASSIGN)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting variable assignment symbol");
		}
		vhdl_getnexttoken();
	
		// should be at expression
		EXPRESSION expr = vhdl_parseexpression();
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_getnexttoken();
	
		constant = new CONSTANTDECLARE();
		constant.identifier = ident;
		constant.subtype = ind;
		constant.expression = expr;
	
		return constant;
	}

	/**
	 * Method to parse a signal declaration and return the pointer to the parse tree.
	 * The form of a signal declaration is:
	 *    signal_declaration :== SIGNAL identifier_list : subtype_indication;
	 * @return the signal declaration parse tree.
	 */
	private SIGNALDECLARE vhdl_parsesignal_declare()
		throws ParseException
	{
		vhdl_getnexttoken();
	
		// parse identifier list
		IDENTLIST signal_list = vhdl_parseident_list();
	
		// should be at colon
		if (vhdl_nexttoken.token != TOKEN_COLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a colon");
		}
		vhdl_getnexttoken();
	
		// parse subtype indication
		SUBTYPEIND ind = vhdl_parsesubtype_indication();
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_getnexttoken();
	
		SIGNALDECLARE signal = new SIGNALDECLARE();
		signal.names = signal_list;
		signal.subtype = ind;
		return signal;
	}
	
	/**
	 * Method to parse a subtype indicatio.
	 * It has the form: subtype_indication :== type_mark [constraint]
	 * @return subtype indication parse tree.
	 */
	private SUBTYPEIND vhdl_parsesubtype_indication()
		throws ParseException
	{
		VNAME type = vhdl_parsename();
		SUBTYPEIND ind = new SUBTYPEIND();
		ind.type = type;
		ind.constraint = null;
		return ind;
	}

	/**
	 * Method to parse a component declaration and return a pointer to the parse tree.
	 * The format of a component declaration is:
	 *    component_declaration :== COMPONENT identifier PORT (local_port_list);
	 *    END COMPONENT ;
	 * Note:  Treat local_port_list as a formal_port_list.
	 * @return pointer to a component declaration, null if an error occurs.
	 */
	private VCOMPONENT vhdl_parsecomponent()
		throws ParseException
	{
		VCOMPONENT compo =  null;
		vhdl_getnexttoken();
	
		// should be component identifier
		TOKENLIST entity = null;
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
		} else
		{
			entity = vhdl_nexttoken;
		}
		vhdl_getnexttoken();
	
		// Need keyword PORT
		if (!vhdl_keysame(vhdl_nexttoken,KEY_PORT))
		   vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword PORT");
		else vhdl_getnexttoken();
	
		// should be left bracket, start of port list
		if (vhdl_nexttoken.token != TOKEN_LEFTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a left bracket");
		}
		vhdl_getnexttoken();
	
		// go through port list
		FPORTLIST ports = vhdl_parseformal_port_list();
		if (ports == null)
		{
			// EMPTY 
		}
	
		// should be pointing to RIGHTBRACKET
		if (vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a right bracket");
		}
		vhdl_getnexttoken();
	
		// should be at semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_getnexttoken();
	
		// Need "END COMPONENT"
		if (!vhdl_keysame(vhdl_nexttoken, KEY_END))
		   vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword END");
		vhdl_getnexttoken();
	
		if (!vhdl_keysame(vhdl_nexttoken, KEY_COMPONENT))
		   vhdl_reporterrormsg(vhdl_nexttoken, "Expecting keyword COMPONENT");
		vhdl_getnexttoken();
	
		// should be at terminating semicolon
		if (vhdl_nexttoken.token != TOKEN_SEMICOLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a semicolon");
		}
		vhdl_getnexttoken();
		compo = new VCOMPONENT();
		compo.name = entity;
		compo.ports = ports;
		return compo;
	}

	/**
	 * Method to parse a formal port list.
	 * A formal port list has the form:
	 *    formal_port_list ::= port_declaration {; port_declaration}
	 *    port_declaration ::= identifier_list : port_mode type_mark
	 *    identifier_list  ::= identifier {, identifier}
	 *    port_mode        ::= [in] | [dot] out | inout | linkage
	 *    type_mark        ::= name
	 * @return the formal port list parse tree (null on error).
	 */
	private FPORTLIST vhdl_parseformal_port_list()
		throws ParseException
	{
		// must be at least one port declaration
		IDENTLIST ilist = vhdl_parseident_list();
		if (ilist == null) return null;
	
		// should be at colon
		if (vhdl_nexttoken.token != TOKEN_COLON)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a colon");
			return null;
		}
		vhdl_getnexttoken();
		// Get port mode
		int mode = vhdl_parseport_mode();
		// should be at type_mark
		VNAME type = vhdl_parsename();
	
		// create port declaration
		FPORTLIST ports = new FPORTLIST();
		ports.names = ilist;
		ports.mode = mode;
		ports.type = type;
		ports.next = null;
		FPORTLIST endport = ports;
	
		while (vhdl_nexttoken.token == TOKEN_SEMICOLON)
		{
			vhdl_getnexttoken();
			ilist = vhdl_parseident_list();
			if (ilist == null) return null;
	
			// should be at colon
			if (vhdl_nexttoken.token != TOKEN_COLON)
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a colon");
				return null;
			}
			vhdl_getnexttoken();
			// Get port mode
			mode = vhdl_parseport_mode();
			// should be at type_mark
			type = vhdl_parsename();
			FPORTLIST newport = new FPORTLIST();
			newport.names = ilist;
			newport.mode = mode;
			newport.type = type;
			newport.next = null;
			endport.next = newport;
			endport = newport;
		}
	
		return ports;
	}
	
	/**
	 * Method to parse a port mode description.
	 * The description has the form:
	 *    port_mode :== [in] | [ dot ] out | inout | linkage
	 * @return type of mode (default to in).
	 */
	private int vhdl_parseport_mode()
		throws ParseException
	{
		int mode = MODE_IN;
		if (vhdl_nexttoken.token == TOKEN_KEYWORD)
		{
			switch (((VKEYWORD)(vhdl_nexttoken.pointer)).num)
			{
				case KEY_IN:
					vhdl_getnexttoken();
					break;
				case KEY_OUT:
					mode = MODE_OUT;
					vhdl_getnexttoken();
					break;
				case KEY_INOUT:
					mode = MODE_INOUT;
					vhdl_getnexttoken();
					break;
				case KEY_LINKAGE:
					mode = MODE_LINKAGE;
					vhdl_getnexttoken();
					break;
				default:
					break;
			}
		}
		return mode;
	}
	
	/**
	 * Method to parse a name.
	 * The form of a name is:
	 *    name :== single_name | concatenated_name | attribute_name
	 * @return the name parse tree.
	 */
	private VNAME vhdl_parsename()
		throws ParseException
	{
		int type = NONAME;
		Object pointer = vhdl_parsesinglename();
	
		switch (vhdl_nexttoken.token)
		{
			case TOKEN_AMPERSAND:
				type = NAME_CONCATENATE;
				CONCATENATEDNAME concat = new CONCATENATEDNAME();
				concat.name = (SINGLENAME)pointer;
				concat.next = null;
				pointer = concat;
				while (vhdl_nexttoken.token == TOKEN_AMPERSAND)
				{
					vhdl_getnexttoken();
					SINGLENAME pointer2 = vhdl_parsesinglename();
					CONCATENATEDNAME concat2 = new CONCATENATEDNAME();
					concat.next = concat2;
					concat2.name = pointer2;
					concat2.next = null;
					concat = concat2;
				}
				break;
			case TOKEN_APOSTROPHE:
				break;
			default:
				type = NAME_SINGLE;
			break;
		}
	
		VNAME name = null;
		if (type != NONAME)
		{
			name = new VNAME();
			name.type = type;
			name.pointer = pointer;
		} else
		{
			vhdl_getnexttoken();
		}
		return name;
	}

	/**
	 * Method to parse a single name.
	 * Single names are of the form:
	 *    single_name :== simple_name | selected_name | indexed_name | slice_name
	 * @return the single name structure.
	 */
	private SINGLENAME vhdl_parsesinglename()
		throws ParseException
	{
		int type = NOSINGLENAME;
		SINGLENAME sname = null;
		Object pointer = vhdl_parsesimplename();
	
		if (vhdl_nexttoken.last.space)
		{
			type = SINGLENAME_SIMPLE;
		} else
		{
			switch (vhdl_nexttoken.token)
			{
				case TOKEN_PERIOD:
					break;
				case TOKEN_LEFTBRACKET:
					// could be a indexed_name or a slice_name
					// but support only indexed names
					vhdl_getnexttoken();
					type = SINGLENAME_INDEXED;
					VNAME nptr = new VNAME();
					nptr.type = NAME_SINGLE;
					SINGLENAME sname2 = new SINGLENAME();
					nptr.pointer = sname2;
					sname2.type = SINGLENAME_SIMPLE;
					sname2.pointer = pointer;
					pointer = vhdl_parseindexedname(PREFIX_NAME, nptr);
					// should be at right bracket
					if (vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
					{
						vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a right bracket");
					}
					vhdl_getnexttoken();
					break;
				default:
					type = SINGLENAME_SIMPLE;
					break;
			}
		}
	
		if (type != NOSINGLENAME)
		{
			sname = new SINGLENAME();
			sname.type = type;
			sname.pointer = pointer;
		} else
		{
			vhdl_getnexttoken();
		}
		return sname;
	}
	
	/**
	 * Method to parse a simple name.
	 * The name has the form:
	 *    simple_name ::= identifier
	 * @return pointer to simple name structure.
	 */
	private SIMPLENAME vhdl_parsesimplename()
		throws ParseException
	{
		SIMPLENAME sname = null;
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
			vhdl_getnexttoken();
			return sname;
		}
		sname = new SIMPLENAME();
		sname.identifier = vhdl_nexttoken;
		vhdl_getnexttoken();
		return sname;
	}
	
	/**
	 * Method to parse an indexed name given its prefix and now at the index.
	 * The form of an indexed name is: indexed_name ::= prefix(expression{, expression})
	 * @param pre_type type of prefix (VNAME or FUNCTION CALL).
	 * @param pre_ptr pointer to prefix structure.
	 * @return pointer to indexed name.
	 */
	private INDEXEDNAME vhdl_parseindexedname(int pre_type, VNAME pre_ptr)
		throws ParseException
	{
		PREFIX prefix = new PREFIX();
		prefix.type = pre_type;
		prefix.pointer = pre_ptr;
		INDEXEDNAME ind = new INDEXEDNAME();
		ind.prefix = prefix;
		ind.expr_list = new EXPRLIST();
		ind.expr_list.expression = vhdl_parseexpression();
		ind.expr_list.next = null;
		EXPRLIST elist = ind.expr_list;
	
		// continue while at a comma
		while (vhdl_nexttoken.token == TOKEN_COMMA)
		{
			vhdl_getnexttoken();
			EXPRLIST newelist = new EXPRLIST();
			newelist.expression = vhdl_parseexpression();
			newelist.next = null;
			elist.next = newelist;
			elist = newelist;
		}
	
		return ind;
	}
	
	/**
	 * Method to parse an expression of the form:
	 *    expression ::= relation {AND relation} | relation {OR relation} | relation {NAND relation} | relation {NOR relation} | relation {XOR relation}
	 * @return the expression structure.
	 */
	private EXPRESSION vhdl_parseexpression()
		throws ParseException
	{
		EXPRESSION exp = new EXPRESSION();
		exp.relation = vhdl_parserelation();
		exp.next = null;
	
		// check for more terms
		int key = 0;
		int logop = NOLOGOP;
		if (vhdl_nexttoken.token == TOKEN_KEYWORD)
		{
			key = ((VKEYWORD)(vhdl_nexttoken.pointer)).num;
			switch (key)
			{
				case KEY_AND:
					logop = LOGOP_AND;
					break;
				case KEY_OR:
					logop = LOGOP_OR;
					break;
				case KEY_NAND:
					logop = LOGOP_NAND;
					break;
				case KEY_NOR:
					logop = LOGOP_NOR;
					break;
				case KEY_XOR:
					logop = LOGOP_XOR;
					break;
				default:
					break;
			}
		}
	
		if (logop != NOLOGOP)
		{
			exp.next = vhdl_parsemorerelations(key, logop);
		}
	
		return exp;
	}
	
	/**
	 * Method to parse a relation.
	 * It has the form:
	 * relation ::= simple_expression [relational_operator simple_expression]
	 * relational_operator ::=    =  |  /=  |  <  |  <=  |  >  |  >=
	 * @return the relation structure.
	 */
	private RELATION vhdl_parserelation()
		throws ParseException
	{
		int relop = NORELOP;
		RELATION relation = new RELATION();
		relation.simple_expr = vhdl_parsesimpleexpression();
		relation.rel_operator = NORELOP;
		relation.simple_expr2 = null;
	
		switch (vhdl_nexttoken.token)
		{
			case TOKEN_EQ:
				relop = RELOP_EQ;
				break;
			case TOKEN_NE:
				relop = RELOP_NE;
				break;
			case TOKEN_LT:
				relop = RELOP_LT;
				break;
			case TOKEN_LE:
				relop = RELOP_LE;
				break;
			case TOKEN_GT:
				relop = RELOP_GT;
				break;
			case TOKEN_GE:
				relop = RELOP_GE;
				break;
			default:
				break;
		}
	
		if (relop != NORELOP)
		{
			relation.rel_operator = relop;
			vhdl_getnexttoken();
			relation.simple_expr2 = vhdl_parsesimpleexpression();
		}
	
		return relation;
	}
	
	/**
	 * Method to parse more relations of an expression.
	 * They have the form: AND | OR | NAND | NOR | XOR  relation
	 * Note:  The logical operator must be the same throughout.
	 * @param key key of logical operator.
	 * @param logop logical operator.
	 * @return pointer to more relations, null if no more.
	 */
	private MRELATIONS vhdl_parsemorerelations(int key, int logop)
		throws ParseException
	{
		MRELATIONS more = null;
	
		if (vhdl_keysame(vhdl_nexttoken, key))
		{
			vhdl_getnexttoken();
			more = new MRELATIONS();
			more.log_operator = logop;
			more.relation = vhdl_parserelation();
			more.next = vhdl_parsemorerelations(key, logop);
		}
		return more;
	}

	/**
	 * Method to parse a simple expression.
	 * It has the form: simple_expression ::= [sign] term {adding_operator term}
	 * @return the simple expression structure.
	 */
	private SIMPLEEXPR vhdl_parsesimpleexpression()
		throws ParseException
	{
		SIMPLEEXPR exp = new SIMPLEEXPR();
	
		// check for optional sign
		if (vhdl_nexttoken.token == TOKEN_PLUS)
		{
			exp.sign = 1;
			vhdl_getnexttoken();
		} else if (vhdl_nexttoken.token == TOKEN_MINUS)
		{
			exp.sign = -1;
			vhdl_getnexttoken();
		} else
		{
			exp.sign = 1;			// default sign
		}
	
		// next is a term
		exp.term = vhdl_parseterm();
	
		// check for more terms
		exp.next = vhdl_parsemoreterms();
		return exp;
	}
	
	/**
	 * Method to parse a term.
	 * It has the form: term ::= factor {multiplying_operator factor}
	 * @return the term structure.
	 */
	private TERM vhdl_parseterm()
		throws ParseException
	{
		TERM term = new TERM();
		term.factor = vhdl_parsefactor();
		term.next = vhdl_parsemorefactors();
		return term;
	}
	
	/**
	 * Method to parse more factors of a term.
	 * The factors have the form:
	 *     multiplying_operator factor
	 * @return pointer to more factors, null if no more.
	 */
	private MFACTORS vhdl_parsemorefactors()
		throws ParseException
	{
		MFACTORS more = null;
		int mulop = NOMULOP;
		if (vhdl_nexttoken.token == TOKEN_STAR)
		{
			mulop = MULOP_MULTIPLY;
		} else if (vhdl_nexttoken.token == TOKEN_SLASH)
		{
			mulop = MULOP_DIVIDE;
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_MOD))
		{
			mulop = MULOP_MOD;
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_REM))
		{
			mulop = MULOP_REM;
		}
		if (mulop != NOMULOP)
		{
			vhdl_getnexttoken();
			more = new MFACTORS();
			more.mul_operator = mulop;
			more.factor = vhdl_parsefactor();
			more.next = vhdl_parsemorefactors();
		}
		return more;
	}
	
	/**
	 * Method to parse a factor of the form:
	 *    factor :== primary [** primary] | ABS primary | NOT primary
	 * @return the factor structure.
	 */
	private FACTOR vhdl_parsefactor()
		throws ParseException
	{
		FACTOR factor = null;
		PRIMARY primary = null;
		PRIMARY primary2 = null;
		int miscop = NOMISCOP;
		if (vhdl_keysame(vhdl_nexttoken, KEY_ABS))
		{
			miscop = MISCOP_ABS;
			vhdl_getnexttoken();
			primary = vhdl_parseprimary();
		} else if (vhdl_keysame(vhdl_nexttoken, KEY_NOT))
		{
			miscop = MISCOP_NOT;
			vhdl_getnexttoken();
			primary = vhdl_parseprimary();
		} else
		{
			primary = vhdl_parseprimary();
			if (vhdl_nexttoken.token == TOKEN_DOUBLESTAR)
			{
				miscop = MISCOP_POWER;
				vhdl_getnexttoken();
				primary2 = vhdl_parseprimary();
			}
		}
		factor = new FACTOR();
		factor.primary = primary;
		factor.misc_operator = miscop;
		factor.primary2 = primary2;
		return factor;
	}
	
	/**
	 * Method to parse a primary of the form:
	 *    primary ::= name | literal | aggregate | concatenation | function_call | type_conversion | qualified_expression | (expression)
	 * @return the primary structure.
	 */
	private PRIMARY vhdl_parseprimary()
		throws ParseException
	{
		int type = NOPRIMARY;
		Object pointer = null;
		PRIMARY primary = null;
		switch (vhdl_nexttoken.token)
		{
			case TOKEN_DECIMAL:
			case TOKEN_BASED:
			case TOKEN_STRING:
			case TOKEN_BIT_STRING:
				type = PRIMARY_LITERAL;
				pointer = vhdl_parseliteral();
				break;
			case TOKEN_IDENTIFIER:
				type = PRIMARY_NAME;
				pointer = vhdl_parsename();
				break;
			case TOKEN_LEFTBRACKET:
				// should be an expression in brackets
				vhdl_getnexttoken();
				type = PRIMARY_EXPRESSION;
				pointer = vhdl_parseexpression();
	
				// should be at right bracket
				if (vhdl_nexttoken.token != TOKEN_RIGHTBRACKET)
				{
					vhdl_reporterrormsg(vhdl_nexttoken, "Expecting a right bracket");
				}
				vhdl_getnexttoken();
				break;
			default:
				break;
		}
		if (type != NOPRIMARY)
		{
			primary = new PRIMARY();
			primary.type = type;
			primary.pointer = pointer;
		}
		return primary;
	}
	
	/**
	 * Method to parse a literal of the form:
	 *    literal ::= numeric_literal | enumeration_literal | string_literal | bit_string_literal
	 * @return pointer to returned literal structure.
	 */
	private LITERAL vhdl_parseliteral()
		throws ParseException
	{
		LITERAL literal = null;
		Object pointer = null;
		int type = NOLITERAL;
		switch(vhdl_nexttoken.token)
		{
			case TOKEN_DECIMAL:
				type = LITERAL_NUMERIC;
				pointer = vhdl_parsedecimal();
				break;
			case TOKEN_BASED:
				// type = LITERAL_NUMERIC;
				// pointer = vhdl_parsebased();
				break;
			case TOKEN_STRING:
				break;
			case TOKEN_BIT_STRING:
				break;
			default:
				break;
		}
		if (type != NOLITERAL)
		{
			literal = new LITERAL();
			literal.type = type;
			literal.pointer = pointer;
		}
		return literal;
	}
	
	/**
	 * Method to parse a decimal literal of the form:
	 *    decimal_literal ::= integer [.integer] [exponent]
	 *    integer ::= digit {[underline] digit}
	 *    exponent ::= E [+] integer | E - integer
	 * Currently only integer supported.
	 * @return the value of decimal literal.
	 */
	private Integer vhdl_parsedecimal()
		throws ParseException
	{
		int value = TextUtils.atoi((String)vhdl_nexttoken.pointer);
		vhdl_getnexttoken();
		return new Integer(value);
	}

	/**
	 * Method to parse more terms of a simple expression.
	 * The terms have the form:
	 *    adding_operator term
	 * @return pointer to more terms, null if no more.
	 */
	private MTERMS vhdl_parsemoreterms()
		throws ParseException
	{
		MTERMS more = null;
		int addop = NOADDOP;
		if (vhdl_nexttoken.token == TOKEN_PLUS)
		{
			addop = ADDOP_ADD;
		} else if (vhdl_nexttoken.token == TOKEN_MINUS)
		{
			addop = ADDOP_SUBTRACT;
		}
		if (addop != NOADDOP)
		{
			vhdl_getnexttoken();
			more = new MTERMS();
			more.add_operator = addop;
			more.term = vhdl_parseterm();
			more.next = vhdl_parsemoreterms();
		}
		return more;
	}

	/**
	 * Method to parse an identifier list and return its parse tree.
	 * The form of an identifier list is:
	 *	  identifier_list :== identifier {, identifier}
	 * @return a pointer to identifier list.
	 */
	private IDENTLIST vhdl_parseident_list()
		throws ParseException
	{
		// must be at least one identifier
		if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
			vhdl_getnexttoken();
			return null;
		}
		IDENTLIST newilist = new IDENTLIST();
		newilist.identifier = vhdl_nexttoken;
		newilist.next = null;
		IDENTLIST ilist = newilist;
		IDENTLIST ilistend = newilist;
	
		// continue while a comma is next
		vhdl_getnexttoken();
		while (vhdl_nexttoken.token == TOKEN_COMMA)
		{
			vhdl_getnexttoken();
			// should be another identifier
			if (vhdl_nexttoken.token != TOKEN_IDENTIFIER)
			{
				vhdl_reporterrormsg(vhdl_nexttoken, "Expecting an identifier");
				vhdl_getnexttoken();
				return null;
			}
			newilist = new IDENTLIST();
			newilist.identifier = vhdl_nexttoken;
			newilist.next = null;
			ilistend.next = newilist;
			ilistend = newilist;
			vhdl_getnexttoken();
		}
		return ilist;
	}

	/**
	 * Method to ignore up to the next semicolon.
	 */
	private void vhdl_parsetosemicolon()
		throws ParseException
	{
		for(;;)
		{
			vhdl_getnexttoken();
			if (vhdl_nexttoken.token == TOKEN_SEMICOLON)
			{
				vhdl_getnexttoken();
				break;
			}
		}
	}
	
	/**
	 * Method to get the next token if possible.
	 */
	private void vhdl_getnexttoken()
		throws ParseException
	{
		if (vhdl_nexttoken.next == null)
		{
			vhdl_reporterrormsg(vhdl_nexttoken, "Unexpected termination within block");
			throw new ParseException();
		}
		vhdl_nexttoken = vhdl_nexttoken.next;
	}
	
	/**
	 * Method to compare the two keywords, the first as part of a token.
	 * @param tokenptr pointer to the token entity.
	 * @param key value of key to be compared.
	 * @return true if the same, false if not the same.
	 */
	private boolean vhdl_keysame(TOKENLIST tokenptr, int key)
	{
		if (tokenptr.token != TOKEN_KEYWORD) return false;
		if (((VKEYWORD)(tokenptr.pointer)).num == key)
		{
			return true;
		}
		return false;
	}

	private void vhdl_reporterrormsg(TOKENLIST tlist, String err_msg)
	{
		vhdl_err = true;
		vhdl_errorcount++;
		if (vhdl_errorcount == 30)
			System.out.println("TOO MANY ERRORS...PRINTING NO MORE");
		if (vhdl_errorcount >= 30) return;
		if (tlist == null)
		{
			System.out.println("ERROR " + err_msg);
			return;
		}
		System.out.println("ERROR on line " + tlist.line_num + ", " + err_msg + ":");
	
		// back up to start of line
		TOKENLIST tstart;
		for (tstart = tlist; tstart.last != null; tstart = tstart.last)
		{
			if (tstart.last.line_num != tlist.line_num) break;
		}
	
		// form line in buffer
		int pointer = 0;
		StringBuffer buffer = new StringBuffer();
		for ( ; tstart != null && tstart.line_num == tlist.line_num; tstart = tstart.next)
		{
			int i = buffer.length();
			if (tstart == tlist) pointer = i;
			if (tstart.token < TOKEN_ARROW)
			{
				char chr = vhdl_delimiterstr.charAt(tstart.token);
				buffer.append(chr);
			} else if (tstart.token < TOKEN_UNKNOWN)
			{
				int start = 2 * (tstart.token - TOKEN_ARROW);
				buffer.append(vhdl_doubledelimiterstr.substring(start, start+2));
			} else switch (tstart.token)
			{
				case TOKEN_STRING:
					buffer.append("\"" + tstart.pointer + "\" ");
					break;
				case TOKEN_KEYWORD:
					buffer.append(((VKEYWORD)tstart.pointer).name);
					break;
				case TOKEN_IDENTIFIER:
					buffer.append(tstart.pointer);
					break;
				case TOKEN_CHAR:
					buffer.append(((Character)tstart.pointer).charValue());
				case TOKEN_DECIMAL:
					buffer.append(tstart.pointer);
					break;
				default:
					if (tstart.pointer != null)
						buffer.append(tstart.pointer);
					break;
			}
			if (tstart.space)buffer.append(" ");
		}
	
		// print out line
		System.out.println(buffer.toString());
	
		// print out pointer
		buffer = new StringBuffer();
		for (int i = 0; i < pointer; i++) buffer.append(" ");
		System.out.println(buffer.toString() + "^");
	}

	/******************************** THE VHDL SEMANTICS ********************************/

	private DBUNITS			vhdl_units;
	private SYMBOLLIST		vhdl_symbols, vhdl_gsymbols;
	private int              vhdl_for_level = 0;
	private int []           vhdl_for_tags = new int[10];
	private String           vhdl_default_name;

	/**
	 * Method to start semantic analysis of the generated parse tree.
	 * @return the status of the analysis (errors).
	 */
	private boolean vhdl_semantic()
	{
		vhdl_err = false;
		vhdl_default_name = "default";
		vhdl_units = new DBUNITS();
		vhdl_units.interfaces = null;
		DBINTERFACE endinterface = null;
		vhdl_units.bodies = null;
		DBBODY endbody = null;
	
		vhdl_symbols = vhdl_pushsymbols(null);
		vhdl_gsymbols = vhdl_pushsymbols(null);
	
		// add defaults to symbol tree
		vhdl_createdefaulttype(vhdl_symbols);
		SYMBOLLIST ssymbols = vhdl_symbols;
	
		vhdl_symbols = vhdl_pushsymbols(vhdl_symbols);
	
		for (PTREE unit = vhdl_ptree; unit != null; unit = unit.next)
		{
			switch (unit.type)
			{
				case UNIT_INTERFACE:
					DBINTERFACE interfacef = vhdl_seminterface((VINTERFACE)unit.pointer);
					if (interfacef == null) break;
					if (endinterface == null)
					{
						vhdl_units.interfaces = endinterface = interfacef;
					} else
					{
						endinterface.next = interfacef;
						endinterface = interfacef;
					}
					vhdl_symbols = vhdl_pushsymbols(ssymbols);
					break;
				case UNIT_BODY:
					DBBODY body = vhdl_sembody((BODY)unit.pointer);
					if (endbody == null)
					{
						vhdl_units.bodies = endbody = body;
					} else
					{
						endbody.next = body;
						endbody = body;
					}
					vhdl_symbols = vhdl_pushsymbols(ssymbols);
					break;
				case UNIT_PACKAGE:
					vhdl_sempackage((PACKAGE)unit.pointer);
					break;
				case UNIT_USE:
					vhdl_semuse((USE)unit.pointer);
					break;
				case UNIT_FUNCTION:
				default:
					break;
			}
		}
	
		return vhdl_err;
	}
	
	/**
	 * Method to do semantic analysis of a use statement.
	 * Add package symbols to symbol list.
	 * @param use pointer to use parse structure.
	 */
	private void vhdl_semuse(USE use)
	{
		for ( ; use != null; use = use.next)
		{
			/* Note this code was lifted with minor mods from vhdl_semwith()
			 * which is not a distinct function in IEEE version.
			 * It seems a little redundant as written, but I don't
			 * really understand what Andy was doing here.....
			 */
			SYMBOLTREE symbol = vhdl_searchsymbol((String)use.unit.pointer, vhdl_gsymbols);
			if (symbol == null)
			{
				continue;
			}
			if (symbol.type != SYMBOL_PACKAGE)
			{
				vhdl_reporterrormsg(use.unit, "Symbol is not a PACKAGE");
			} else
			{
				vhdl_addsymbol(symbol.value, SYMBOL_PACKAGE, symbol.pointer, vhdl_symbols);
			}
			symbol = vhdl_searchsymbol((String)use.unit.pointer, vhdl_gsymbols);
			if (symbol == null)
			{
				vhdl_reporterrormsg(use.unit, "Symbol is undefined");
				continue;
			}
			if (symbol.type != SYMBOL_PACKAGE)
			{
				vhdl_reporterrormsg(use.unit, "Symbol is not a PACKAGE");
			} else
			{
				SYMBOLLIST new_sym_list = ((DBPACKAGE)(symbol.pointer)).root;
				new_sym_list.last = vhdl_symbols;
				vhdl_symbols = new_sym_list;
			}
		}
	}

	/**
	 * Method to do semantic analysis of a package declaration.
	 * @param vpackage pointer to a package.
	 */
	private void vhdl_sempackage(PACKAGE vpackage)
	{
		if (vpackage == null) return;
		DBPACKAGE dbpackage = null;
	
		// search to see if package name is unique
		if (vhdl_searchsymbol((String)vpackage.name.pointer, vhdl_gsymbols) != null)
		{
			vhdl_reporterrormsg(vpackage.name, "Symbol previously defined");
		} else
		{
			dbpackage = new DBPACKAGE();
			dbpackage.name = (String)vpackage.name.pointer;
			dbpackage.root = null;
			vhdl_addsymbol(dbpackage.name, SYMBOL_PACKAGE, dbpackage, vhdl_gsymbols);
		}
	
		// check package parts
		vhdl_symbols = vhdl_pushsymbols(vhdl_symbols);
		for (PACKAGEDPART part = vpackage.declare; part != null; part = part.next)
		{
			vhdl_sembasic_declare(part.item);
		}
		if (dbpackage != null)
		{
			dbpackage.root = vhdl_symbols;
		}
		vhdl_symbols = vhdl_popsymbols(vhdl_symbols);
	}

	/**
	 * Method to do semantic analysis of an body declaration.
	 * @param body pointer to body parse structure.
	 * @return resultant database body.
	 */
	private DBBODY vhdl_sembody(BODY body)
	{
		DBBODY dbbody = null;
		if (body == null) return dbbody;
		if (vhdl_searchsymbol((String)body.name.pointer, vhdl_gsymbols) != null)
		{
			vhdl_reporterrormsg(body.name, "Body previously defined");
			return dbbody;
		}
	
		// create dbbody
		dbbody = new DBBODY();
		dbbody.classnew = body.classnew;
		dbbody.name = (String)body.name.pointer;
		dbbody.entity = null;
		dbbody.declare = null;
		dbbody.statements = null;
		dbbody.parent = null;
		dbbody.same_parent = null;
		dbbody.next = null;
		vhdl_addsymbol(dbbody.name, SYMBOL_BODY, dbbody, vhdl_gsymbols);
	
		// check if interface declared
		SYMBOLTREE symbol = vhdl_searchsymbol((String)body.entity.identifier.pointer, vhdl_gsymbols);
		if (symbol == null)
		{
			vhdl_reporterrormsg((TOKENLIST)body.entity.identifier, "Reference to undefined entity");
			return dbbody;
		} else if (symbol.type != SYMBOL_ENTITY)
		{
			vhdl_reporterrormsg((TOKENLIST)body.entity.identifier, "Symbol is not an entity");
			return dbbody;
		} else
		{
			dbbody.entity = symbol.value;
			dbbody.parent = (DBINTERFACE)symbol.pointer;
			if (symbol.pointer != null)
			{
				// add interfacef-body reference to list
				dbbody.same_parent = ((DBINTERFACE)(symbol.pointer)).bodies;
				((DBINTERFACE)(symbol.pointer)).bodies = dbbody;
			}
		}
	
		// create new symbol tree
		SYMBOLLIST temp_symbols = vhdl_symbols;
		SYMBOLLIST endsymbol = vhdl_symbols;
		if (symbol.pointer != null)
		{
			while (endsymbol.last != null)
			{
				endsymbol = endsymbol.last;
			}
			endsymbol.last = ((DBINTERFACE)(symbol.pointer)).symbols;
		}
		vhdl_symbols = vhdl_pushsymbols(vhdl_symbols);
	
		// check body declaration
		dbbody.declare = vhdl_sembody_declare(body.body_declare);
	
		// check statements
		dbbody.statements = vhdl_semset_of_statements(body.statements);
	
		// delete current symbol table
		vhdl_symbols = temp_symbols;
		endsymbol.last = null;
	
		return dbbody;
	}
	
	/**
	 * Method to do semantic analysis of architectural set of statements in a body.
	 * @param state pointer to architectural statements.
	 * @return pointer to created statements.
	 */
	private DBSTATEMENTS vhdl_semset_of_statements(STATEMENTS state)
	{
		if (state == null) return null;
		DBSTATEMENTS dbstates = new DBSTATEMENTS();
		dbstates.instances = null;
		DBINSTANCE endinstance = null;
		for (; state != null; state = state.next)
		{
			switch (state.type)
			{
				case ARCHSTATE_INSTANCE:
					DBINSTANCE newinstance = vhdl_seminstance((VINSTANCE)state.pointer);
					if (endinstance == null)
					{
						dbstates.instances = endinstance = newinstance;
					} else
					{
						endinstance.next = newinstance;
						endinstance = newinstance;
					}
					break;
				case ARCHSTATE_GENERATE:
					DBSTATEMENTS newstate = vhdl_semgenerate((GENERATE)state.pointer);
					if (newstate != null)
					{
						for (newinstance = newstate.instances; newinstance != null;
							newinstance = newinstance.next)
						{
							if (endinstance == null)
							{
								dbstates.instances = endinstance = newinstance;
							} else
							{
								endinstance.next = newinstance;
								endinstance = newinstance;
							}
						}
					}
					break;
				case ARCHSTATE_SIG_ASSIGN:
				case ARCHSTATE_IF:
				case ARCHSTATE_CASE:
				default:
					break;
			}
		}
		return dbstates;
	}
	
	/**
	 * Method to do semantic analysis of generate statement.
	 * @param gen pointer to generate statement.
	 * @return pointer to generated statements.
	 */
	private DBSTATEMENTS vhdl_semgenerate(GENERATE gen)
	{
		DBSTATEMENTS dbstates = null;
		if (gen == null) return dbstates;
	
		// check label
		// For IEEE standard, check label only if not inside a for generate
		// Not a perfect implementation, but label is not used for anything
		// in this situation.  This is easier to check in the parser...    
		if (gen.label != null && vhdl_for_level == 0)
		{
			// check label for uniqueness
			if (vhdl_searchsymbol((String)gen.label.pointer, vhdl_symbols) != null)
			{
				vhdl_reporterrormsg(gen.label, "Symbol previously defined");
			} else
			{
				vhdl_addsymbol((String)gen.label.pointer, SYMBOL_LABEL, null, vhdl_symbols);
				vhdl_default_name = (String)gen.label.pointer;
			}
		}
	
		// check generation scheme
		GENSCHEME scheme = gen.gen_scheme;
		if (scheme == null)
			return dbstates;
		switch (scheme.scheme)
		{
			case GENSCHEME_FOR:
	
				// Increment vhdl_for_level and clear tag
				vhdl_for_tags[++vhdl_for_level] = 0;

				// create new local symbol table
				vhdl_symbols = vhdl_pushsymbols(vhdl_symbols);
	
				// add identifier as a variable symbol
				SYMBOLTREE symbol = vhdl_addsymbol((String)scheme.identifier.pointer, SYMBOL_VARIABLE,
					null, vhdl_symbols);
	
				// determine direction of discrete range (ascending or descending)
				DBDISCRETERANGE drange = vhdl_semdiscrete_range(scheme.range);
				if (drange.start > drange.end)
				{
					int temp = drange.end;
					drange.end = drange.start;
					drange.start = temp;
				}
				DBSTATEMENTS oldstates = null;
				DBINSTANCE endinst = null;
				for (int temp = drange.start; temp <= drange.end; temp++)
				{
					symbol.pointer = new Integer(temp);
					dbstates = vhdl_semset_of_statements(gen.statements);
					++vhdl_for_tags[vhdl_for_level];
					if (dbstates != null)
					{
						if (oldstates == null)
						{
							oldstates = dbstates;
							endinst = dbstates.instances;
							if (endinst != null)
							{
								while (endinst.next != null)
								{
									endinst = endinst.next;
								}
							}
						} else
						{
							for (DBINSTANCE inst = dbstates.instances; inst != null; inst = inst.next)
							{
								if (endinst == null)
								{
									oldstates.instances = endinst = inst;
								} else
								{
									endinst.next = inst;
									endinst = inst;
								}
							}
						}
					}
				}
				dbstates = oldstates;
	
				// restore old symbol table
				vhdl_symbols = vhdl_popsymbols(vhdl_symbols);
				--vhdl_for_level;
				break;
	
			case GENSCHEME_IF:
				if (vhdl_evalexpression(scheme.condition) != 0)
				{
					dbstates = vhdl_semset_of_statements(gen.statements);
				}
			default:
				break;
		}
		return dbstates;
	}
	
	/**
	 * Method to do demantic analysis of instance for an architectural body.
	 * @param inst pointer to instance structure.
	 * @return pointer to created instance.
	 */
	private DBINSTANCE vhdl_seminstance(VINSTANCE inst)
	{
		DBINSTANCE dbinst = null;
		if (inst == null) return dbinst;
		// If inside a "for generate" make unique instance name
		// from instance label and vhdl_for_tags[]
		
		String ikey = null;
		if (vhdl_for_level > 0)
		{
			if (inst.name == null) ikey = "no_name"; else
				ikey = (String)inst.name.pointer;
			for (int i=1; i<=vhdl_for_level; ++i)
			{
				ikey += "_" + vhdl_for_tags[i];
			}
		} else
		{
			ikey = (String)inst.name.pointer;
		}
		dbinst = new DBINSTANCE();
		dbinst.name = ikey;
		dbinst.compo = null;
		dbinst.ports = null;
		DBAPORTLIST enddbaport = null;
		dbinst.next = null;
		if (vhdl_searchsymbol(dbinst.name, vhdl_symbols) != null)
		{
			vhdl_reporterrormsg(inst.name, "Instance name previously defined");
		} else
		{
			vhdl_addsymbol(dbinst.name, SYMBOL_INSTANCE, dbinst, vhdl_symbols);
		}
	
		// check that instance entity is among component list
		DBCOMPONENTS compo = null;
		SYMBOLTREE symbol = vhdl_searchsymbol((String)inst.entity.identifier.pointer, vhdl_symbols);
		if (symbol == null)
		{
			vhdl_reporterrormsg(inst.entity.identifier, "Instance references undefined component");
		} else if (symbol.type != SYMBOL_COMPONENT)
		{
			vhdl_reporterrormsg(inst.entity.identifier, "Symbol is not a component reference");
		} else
		{
			compo = (DBCOMPONENTS)symbol.pointer;
			dbinst.compo = compo;
	
			// check that number of ports match
			int iport_num = 0;
			for (APORTLIST aplist = inst.ports; aplist != null; aplist = aplist.next)
			{
				iport_num++;
			}
			int cport_num = 0;
			for (DBPORTLIST plist = compo.ports; plist != null; plist = plist.next)
			{
				cport_num++;
			}
			if (iport_num != cport_num)
			{
				vhdl_reporterrormsg(vhdl_getnametoken((VNAME)inst.ports.pointer),
					"Instance has different number of ports that component");
				return null;
			}
		}
	
		// check that ports of instance are either signals or entity port
		// note 0 ports are allowed for position placement
		DBPORTLIST plist = null;
		if (compo != null)
		{
			plist = compo.ports;
		}
		for (APORTLIST aplist = inst.ports; aplist != null; aplist = aplist.next)
		{
			DBAPORTLIST dbaport = new DBAPORTLIST();
			dbaport.name = null;
			dbaport.port = plist;
			if (plist != null)
			{
				plist = plist.next;
			}
			dbaport.flags = 0;
			dbaport.next = null;
			if (enddbaport == null)
			{
				dbinst.ports = enddbaport = dbaport;
			} else
			{
				enddbaport.next = dbaport;
				enddbaport = dbaport;
			}
			if (aplist.pointer == null) continue;
			dbaport.name = vhdl_semname((VNAME)aplist.pointer);
	
			// check that name is reference to a signal or formal port
			vhdl_semaport_check((VNAME)aplist.pointer);
		}
	
		return dbinst;
	}
	
	/**
	 * Method to do semantic analysis of a name.
	 * @param name pointer to name structure.
	 * @return pointer to created db name.
	 */
	private DBNAME vhdl_semname(VNAME name)
	{
		DBNAME dbname = null;
		if (name == null) return dbname;
		switch (name.type)
		{
			case NAME_SINGLE:
				dbname = vhdl_semsinglename((SINGLENAME)name.pointer);
				break;
			case NAME_CONCATENATE:
				dbname = vhdl_semconcatenatedname((CONCATENATEDNAME)name.pointer);
				break;
			case NAME_ATTRIBUTE:
			default:
				break;
		}
		return dbname;
	}
	
	/**
	 * Method to do semantic analysis of a concatenated name.
	 * @param name pointer to concatenated name structure.
	 * @return pointer to generated db name.
	 */
	private DBNAME vhdl_semconcatenatedname(CONCATENATEDNAME name)
	{
		DBNAME dbname = null;
		if (name == null) return dbname;
		dbname = new DBNAME();
		dbname.name = null;
		dbname.type = DBNAME_CONCATENATED;
		dbname.pointer = null;
		dbname.dbtype = null;
		DBNAMELIST end = null;
		for (CONCATENATEDNAME cat = name; cat != null; cat = cat.next)
		{
			DBNAMELIST newnl = new DBNAMELIST();
			newnl.name = vhdl_semsinglename(cat.name);
			newnl.next = null;
			if (end != null)
			{
				end.next = newnl;
				end = newnl;
			} else
			{
				end = newnl;
				dbname.pointer = newnl;
			}
		}
		return dbname;
	}

	/**
	 * Method to do semantic analysis of a single name.
	 * @param name pointer to single name structure.
	 * @return pointer to generated db name.
	 */
	private DBNAME vhdl_semsinglename(SINGLENAME name)
	{
		DBNAME dbname = null;
		if (name == null) return dbname;
		switch (name.type)
		{
			case SINGLENAME_SIMPLE:
				dbname = new DBNAME();
				dbname.name = (String)((SIMPLENAME)(name.pointer)).identifier.pointer;
				dbname.type = DBNAME_IDENTIFIER;
				dbname.pointer = null;
				dbname.dbtype = vhdl_gettype(dbname.name);
				break;
			case SINGLENAME_INDEXED:
				dbname = vhdl_semindexedname((INDEXEDNAME)name.pointer);
				break;
			case SINGLENAME_SLICE:
			case SINGLENAME_SELECTED:
			default:
				break;
		}
		return dbname;
	}
	
	/**
	 * Method to do semantic analysis of an indexed name.
	 * @param name pointer to indexed name structure.
	 * @return pointer to generated name.
	 */
	private DBNAME vhdl_semindexedname(INDEXEDNAME name)
	{
		DBNAME dbname = null;
		if (name == null) return dbname;
	
		// must be an array type
		DBLTYPE type = vhdl_gettype(vhdl_getprefixident(name.prefix));
		if (type == null)
		{

			vhdl_reporterrormsg(vhdl_getprefixtoken(name.prefix), "No type specified");
			return dbname;
		}
		if (type.type != DBTYPE_ARRAY)
		{
			vhdl_reporterrormsg(vhdl_getprefixtoken(name.prefix), "Must be of constrained array type");
			return dbname;
		}
		dbname = new DBNAME();
		dbname.name = vhdl_getprefixident(name.prefix);
		dbname.type = DBNAME_INDEXED;
		dbname.pointer = null;
		dbname.dbtype = type;
	
		// evaluate any expressions
		DBINDEXRANGE indexr = (DBINDEXRANGE)type.pointer;
		DBEXPRLIST dbexpr = null, endexpr = null;
		for (EXPRLIST expr = name.expr_list; expr != null && indexr != null; expr = expr.next)
		{
			int value = vhdl_evalexpression(expr.expression);
			if (!vhdl_indiscreterange(value, indexr.drange))
			{
				vhdl_reporterrormsg(vhdl_getprefixtoken(name.prefix), "Index is out of range");
				return(dbname);
			}
			DBEXPRLIST nexpr = new DBEXPRLIST();
			nexpr.value = value;
			nexpr.next = null;
			if (endexpr == null)
			{
				dbexpr = endexpr = nexpr;
			} else
			{
				endexpr.next = nexpr;
				endexpr = nexpr;
			}
			indexr = indexr.next;
		}
		dbname.pointer = dbexpr;
		return dbname;
	}
	
	/**
	 * Method to decide whether value is in discrete range.
	 * @param value value to be checked.
	 * @param discrete pointer to db discrete range structure.
	 * @return true if value in discrete range, else false.
	 */
	private boolean vhdl_indiscreterange(int value, DBDISCRETERANGE discrete)
	{
		boolean in_range = false;
		if (discrete == null)
			return in_range;
		int start = discrete.start;
		int end = discrete.end;
		if (start > end)
		{
			int temp = end;
			end = start;
			start = temp;
		}
		if (value >= start && value <= end)
			in_range = true;
		return in_range;
	}

	/**
	 * Method to get the type of an identifier.
	 * @param ident pointer to identifier.
	 * @return type, null if no type.
	 */
	private DBLTYPE vhdl_gettype(String ident)
	{
		DBLTYPE type = null;
		if (ident != null)
		{
			SYMBOLTREE symbol = vhdl_searchsymbol(ident, vhdl_symbols);
			if (symbol != null)
			{
				type = vhdl_getsymboltype(symbol);
			}
		}
	
		return type;
	}
	
	/**
	 * Method to get a pointer to the type of a symbol.
	 * @param symbol pointer to symbol.
	 * @return pointer to returned type, null if no type exists.
	 */
	private DBLTYPE vhdl_getsymboltype(SYMBOLTREE symbol)
	{
		DBLTYPE type = null;
		if (symbol == null) return type;
		switch (symbol.type)
		{
			case SYMBOL_FPORT:
				DBPORTLIST fport = (DBPORTLIST)symbol.pointer;
				if (fport == null) break;
				type = fport.type;
				break;
			case SYMBOL_SIGNAL:
				DBSIGNALS signal = (DBSIGNALS)symbol.pointer;
				if (signal == null) break;
				type = signal.type;
				break;
			case SYMBOL_TYPE:
				type = (DBLTYPE)symbol.pointer;
				break;
			default:
				break;
		}
		return type;
	}

	/**
	 * Method to check that the passed name which is a reference on an actual port
	 * list is a signal of formal port.
	 * @param name pointer to name parse structure.
	 */
	private void vhdl_semaport_check(VNAME name)
	{
		switch (name.type)
		{
			case NAME_SINGLE:
				vhdl_semaport_check_single_name((SINGLENAME)name.pointer);
				break;
			case NAME_CONCATENATE:
				for (CONCATENATEDNAME cat = (CONCATENATEDNAME)name.pointer; cat != null; cat = cat.next)
				{
					vhdl_semaport_check_single_name(cat.name);
				}
				break;
			default:
				break;
		}
	}
	
	/**
	 * Method to check that the passed single name references a signal or formal port.
	 * @param sname pointer to single name structure.
	 */
	private void vhdl_semaport_check_single_name(SINGLENAME sname)
	{
		switch (sname.type)
		{
			case SINGLENAME_SIMPLE:
				SIMPLENAME simname = (SIMPLENAME)sname.pointer;
				String ident = (String)simname.identifier.pointer;
				SYMBOLTREE symbol = vhdl_searchsymbol(ident, vhdl_symbols);
				if (symbol == null ||
					(symbol.type != SYMBOL_FPORT && symbol.type != SYMBOL_SIGNAL))
				{
					vhdl_reporterrormsg(simname.identifier,
						"Instance port has reference to unknown port");
				}
				break;
			case SINGLENAME_INDEXED:
				INDEXEDNAME iname = (INDEXEDNAME)sname.pointer;
				ident = (String)vhdl_getprefixident(iname.prefix);
				symbol = vhdl_searchsymbol(ident, vhdl_symbols);
				if (symbol == null)
				{
					symbol = vhdl_searchsymbol(ident, vhdl_symbols);
				}
				if (symbol == null ||
					(symbol.type != SYMBOL_FPORT && symbol.type != SYMBOL_SIGNAL))
				{
					vhdl_reporterrormsg(vhdl_getprefixtoken(iname.prefix),
						"Instance port has reference to unknown port");
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Method to do semantic analysis of the body declaration portion of a body.
	 * @param ointer to body declare. pointer to body declare.
	 * @return pointer to generated body declare.
	 */
	private DBBODYDECLARE vhdl_sembody_declare(BODYDECLARE declare)
	{
		DBBODYDECLARE dbdeclare = null;
		if (declare == null) return dbdeclare;
		dbdeclare = new DBBODYDECLARE();
		dbdeclare.components = null;
		DBCOMPONENTS endcomponent = null;
		dbdeclare.bodysignals = null;
		DBSIGNALS endsignal = null;
	
		for (; declare != null; declare = declare.next)
		{
			switch (declare.type)
			{
				case BODYDECLARE_BASIC:
					DBSIGNALS newsignals = vhdl_sembasic_declare((BASICDECLARE)declare.pointer);
					if (newsignals != null)
					{
						if (endsignal == null)
						{
							dbdeclare.bodysignals = endsignal = newsignals;
						} else
						{
							endsignal.next = newsignals;
							endsignal = newsignals;
						}
						while (endsignal.next != null)
						{
							endsignal = endsignal.next;
						}
					}
					break;
				case BODYDECLARE_COMPONENT:
					DBCOMPONENTS newcomponent = vhdl_semcomponent((VCOMPONENT)declare.pointer);
					if (newcomponent != null)
					{
						if (endcomponent == null)
						{
							dbdeclare.components = endcomponent = newcomponent;
						} else
						{
							endcomponent.next = newcomponent;
							endcomponent = newcomponent;
						}
					}
					break;
				case BODYDECLARE_RESOLUTION:
				case BODYDECLARE_LOCAL:
				default:
					break;
			}
		}
	
		return dbdeclare;
	}
	
	/**
	 * Method to do semantic analysis of body's component.
	 * @param compo pointer to component parse.
	 * @return pointer to created component.
	 */
	private DBCOMPONENTS vhdl_semcomponent(VCOMPONENT compo)
	{
		DBCOMPONENTS dbcomp = null;
		if (compo == null) return dbcomp;
		if (vhdl_searchfsymbol((String)compo.name.pointer, vhdl_symbols) != null)
		{
			vhdl_reporterrormsg(compo.name, "Identifier previously defined");
			return dbcomp;
		}
		dbcomp = new DBCOMPONENTS();
		dbcomp.name = (String)compo.name.pointer;
		dbcomp.ports = null;
		dbcomp.next = null;
		vhdl_addsymbol(dbcomp.name, SYMBOL_COMPONENT, dbcomp, vhdl_symbols);
		vhdl_symbols = vhdl_pushsymbols(vhdl_symbols);
		dbcomp.ports = vhdl_semformal_port_list(compo.ports);
		vhdl_symbols = vhdl_popsymbols(vhdl_symbols);
		return dbcomp;
	}
	
	/**
	 * Method to top off the top most symbol list and return next symbol list.
	 * @param old_sym_list pointer to old symbol list.
	 * @return pointer to new symbol list.
	 */
	private SYMBOLLIST vhdl_popsymbols(SYMBOLLIST old_sym_list)
	{
		if (old_sym_list == null)
		{
			System.out.println("ERROR - trying to pop nonexistant symbol list.");
			return null;
		}
		SYMBOLLIST new_sym_list = old_sym_list.last;
		return new_sym_list;
	}

	/**
	 * Method to do semantic analysis of basic declaration.
	 * @param declare pointer to basic declaration structure.
	 * @return pointer to new signal, null if not.
	 */
	private DBSIGNALS vhdl_sembasic_declare(BASICDECLARE declare)
	{
		DBSIGNALS dbsignal = null;
		if (declare == null) return dbsignal;
		switch (declare.type)
		{
			case BASICDECLARE_OBJECT:
				dbsignal = vhdl_semobject_declare((OBJECTDECLARE)declare.pointer);
				break;
			case BASICDECLARE_TYPE:
				vhdl_semtype_declare((TYPE)declare.pointer);
				break;
			case BASICDECLARE_SUBTYPE:
			case BASICDECLARE_CONVERSION:
			case BASICDECLARE_ATTRIBUTE:
			case BASICDECLARE_ATT_SPEC:
			default:
				break;
		}
		return dbsignal;
	}
	
	/**
	 * Method to do semantic analysis of a type declaration.
	 * @param type pointer to type parse tree.
	 */
	private void vhdl_semtype_declare(TYPE type)
	{
		DBLTYPE dbtype = null;
		if (type == null) return;
	
		// check that type name is distict
		if (vhdl_searchsymbol((String)type.identifier.pointer, vhdl_symbols) != null)
		{
			vhdl_reporterrormsg(type.identifier, "Identifier previously defined");
			return;
		}
	
		// check type definition
		switch (type.type)
		{
			case TYPE_SCALAR:
				break;
			case TYPE_COMPOSITE:
				dbtype = vhdl_semcomposite_type((COMPOSITE)type.pointer);
				break;
			default:
				break;
		}
	
		// add symbol to list
		if (dbtype != null)
		{
			dbtype.name = (String)type.identifier.pointer;
			vhdl_addsymbol(dbtype.name, SYMBOL_TYPE, dbtype, vhdl_symbols);
		}
	}
	
	/**
	 * Method to do semantic analysis of a composite type definition.
	 * @param composite pointer to composite type structure.
	 * @return generated db type.
	 */
	private DBLTYPE vhdl_semcomposite_type(COMPOSITE composite)
	{
		DBLTYPE dbtype = null;
		if (composite == null) return dbtype;
		switch (composite.type)
		{
			case COMPOSITE_ARRAY:
				dbtype = vhdl_semarray_type((ARRAY)composite.pointer);
				break;
			case COMPOSITE_RECORD:
				break;
			default:
				break;
		}
		return dbtype;
	}
	
	/**
	 * Method to do semantic analysis of an array composite type definition.
	 * @param array pointer to composite array type structure.
	 * @return pointer to generated type.
	 */
	private DBLTYPE vhdl_semarray_type(ARRAY array)
	{
		DBLTYPE dbtype = null;
		if (array == null) return dbtype;
		switch (array.type)
		{
			case ARRAY_UNCONSTRAINED:
				break;
			case ARRAY_CONSTRAINED:
				dbtype = vhdl_semconstrained_array((CONSTRAINED)array.pointer);
				break;
			default:
				break;
		}
		return dbtype;
	}
	
	/**
	 * Method to do semantic analysis of a composite constrained array type definition.
	 * @param constr pointer to constrained array structure.
	 * @return pointer to generated type.
	 */
	private DBLTYPE vhdl_semconstrained_array(CONSTRAINED constr)
	{
		DBLTYPE dbtype = null;
		if (constr == null) return dbtype;
		dbtype = new DBLTYPE();
		dbtype.name = null;
		dbtype.type = DBTYPE_ARRAY;
		DBINDEXRANGE endrange = null;
		dbtype.pointer = null;
		dbtype.subtype = null;
	
		// check index constraint
		for (INDEXCONSTRAINT indexc = constr.constraint; indexc != null; indexc = indexc.next)
		{
			DBINDEXRANGE newrange = new DBINDEXRANGE();
			newrange.drange = vhdl_semdiscrete_range(indexc.discrete);
			newrange.next = null;
			if (endrange == null)
			{
				endrange = newrange;
				dbtype.pointer = newrange;
			} else
			{
				endrange.next = newrange;
				endrange = newrange;
			}
		}
		// check subtype indication
		dbtype.subtype = vhdl_semsubtype_indication(constr.subtype);
	
		return dbtype;
	}
	
	/**
	 * Method to do semantic analysis of a sybtype indication.
	 * @param subtype pointer to subtype indication.
	 * @return pointer to db type;
	 */
	private DBLTYPE vhdl_semsubtype_indication(SUBTYPEIND subtype)
	{
		DBLTYPE dbtype = null;
		if (subtype == null) return dbtype;
		dbtype = vhdl_semtype_mark(subtype.type);
		return dbtype;
	}
	
	/**
	 * Method to do semantic type mark.
	 * @param name pointer to type name.
	 * @return pointer to db type.
	 */
	private DBLTYPE vhdl_semtype_mark(VNAME name)
	{
		DBLTYPE dbtype = null;
		if (name == null) return dbtype;
		SYMBOLTREE symbol = vhdl_searchsymbol(vhdl_getnameident(name), vhdl_symbols);
		if (symbol == null ||
			symbol.type != SYMBOL_TYPE)
		{
			vhdl_reporterrormsg(vhdl_getnametoken(name), "Bad type");
		} else
		{
			dbtype = (DBLTYPE)symbol.pointer;
		}
		return dbtype;
	}

	/**
	 * Method to do semantic analysis of a discrete range.
	 * @param discrete pointer to a discrete range structure.
	 * @return pointer to generated range.
	 */
	private DBDISCRETERANGE vhdl_semdiscrete_range(DISCRETERANGE discrete)
	{
		DBDISCRETERANGE dbrange = null;
		if (discrete == null) return dbrange;
		switch (discrete.type)
		{
			case DISCRETERANGE_SUBTYPE:
				break;
			case DISCRETERANGE_RANGE:
				dbrange = vhdl_semrange((RANGE)discrete.pointer);
				break;
			default:
				break;
		}
		return dbrange;
	}
	
	/**
	 * Method to do semantic analysis of a range.
	 * @param range pointer to a range structure.
	 * @return pointer to generated range.
	 */
	private DBDISCRETERANGE vhdl_semrange(RANGE range)
	{
		DBDISCRETERANGE dbrange = null;
		if (range == null) return dbrange;
		switch (range.type)
		{
			case RANGE_ATTRIBUTE:
				break;
			case RANGE_SIMPLE_EXPR:
				RANGESIMPLE rsimp = (RANGESIMPLE)range.pointer;
				if (rsimp != null)
				{
					dbrange = new DBDISCRETERANGE();
					dbrange.start = vhdl_evalsimpleexpr(rsimp.start);
					dbrange.end = vhdl_evalsimpleexpr(rsimp.end);
				}
				break;
			default:
				break;
		}
		return dbrange;
	}

	/**
	 * Method to do semantic analysis of object declaration.
	 * @param primary pointer to object declaration structure.
	 * @return pointer to new signals, null if not.
	 */
	private DBSIGNALS vhdl_semobject_declare(OBJECTDECLARE declare)
	{
		DBSIGNALS signals = null;
		if (declare == null) return signals;
		switch (declare.type)
		{
			case OBJECTDECLARE_SIGNAL:
				signals = vhdl_semsignal_declare((SIGNALDECLARE)declare.pointer);
				break;
			case OBJECTDECLARE_CONSTANT:
				vhdl_semconstant_declare((CONSTANTDECLARE)declare.pointer);
				break;
			case OBJECTDECLARE_VARIABLE:
			case OBJECTDECLARE_ALIAS:
			default:
				break;
		}
		return signals;
	}
	
	/**
	 * Method to do semantic analysis of constant declaration.
	 * @param constant pointer to constant declare structure.
	 */
	private void vhdl_semconstant_declare(CONSTANTDECLARE constant)
	{
		if (constant == null) return;
	
		// check if name exists in top level of symbol tree
		if (vhdl_searchfsymbol((String)constant.identifier.pointer, vhdl_symbols) != null)
		{
			vhdl_reporterrormsg(constant.identifier, "Symbol previously defined");
		} else
		{
			int value = vhdl_evalexpression(constant.expression);
			vhdl_addsymbol((String)constant.identifier.pointer, SYMBOL_CONSTANT,
				new Integer(value), vhdl_symbols);
		}
	}
	
	/**
	 * Method to get the value of an expression.
	 * @param expr pointer to expression structure.
	 * @return value.
	 */
	private int vhdl_evalexpression(EXPRESSION expr)
	{
		if (expr == null) return 0;
		int value = vhdl_evalrelation(expr.relation);
		if (expr.next != null)
		{
			if (value != 0) value = 1;
		}
		for (MRELATIONS more = expr.next; more != null; more = more.next)
		{
			int value2 = vhdl_evalrelation(more.relation);
			if (value2 != 0) value2 = 1;
			switch (more.log_operator)
			{
				case LOGOP_AND:
					value &= value2;
					break;
				case LOGOP_OR:
					value |= value2;
					break;
				case LOGOP_NAND:
					value = ~(value & value2);
					break;
				case LOGOP_NOR:
					value = ~(value | value2);
					break;
				case LOGOP_XOR:
					value ^= value2;
					break;
				default:
					break;
			}
		}
		return value;
	}

	/**
	 * Method to evaluate a relation.
	 * @param relation pointer to relation structure.
	 * @return evaluated value.
	 */
	private int vhdl_evalrelation(RELATION relation)
	{
		if (relation == null) return 0;
		int value = vhdl_evalsimpleexpr(relation.simple_expr);
		if (relation.rel_operator != NORELOP)
		{
			int value2 = vhdl_evalsimpleexpr(relation.simple_expr2);
			switch (relation.rel_operator)
			{
				case RELOP_EQ:
					if (value == value2) value = 1; else
						value = 0;
					break;
				case RELOP_NE:
					if (value != value2) value = 1; else
						value = 0;
					break;
				case RELOP_LT:
					if (value < value2) value = 1; else
						value = 0;
					break;
				case RELOP_LE:
					if (value <= value2) value = 1; else
						value = 0;
					break;
				case RELOP_GT:
					if (value > value2) value = 1; else
						value = 0;
					break;
				case RELOP_GE:
					if (value >= value2) value = 1; else
						value = 0;
					break;
				default:
					break;
			}
		}
		return value;
	}
	
	/**
	 * Method to get the value of a simple expression.
	 * @param expr pointer to a simple expression.
	 * @return value.
	 */
	private int vhdl_evalsimpleexpr(SIMPLEEXPR expr)
	{
		if (expr == null) return 0;
		int value = vhdl_evalterm(expr.term) * expr.sign;
		for (MTERMS more = expr.next; more != null; more = more.next)
		{
			int value2 = vhdl_evalterm(more.term);
			switch (more.add_operator)
			{
				case ADDOP_ADD:
					value += value2;
					break;
				case ADDOP_SUBTRACT:
					value -= value2;
					break;
				default:
					break;
			}
		}
		return value;
	}
	
	/**
	 * Method to get the value of a term.
	 * @param term pointer to a term.
	 * @return value.
	 */
	private int vhdl_evalterm(TERM term)
	{
		if (term == null) return 0;
		int value = vhdl_evalfactor(term.factor);
		for (MFACTORS more = term.next; more != null; more = more.next)
		{
			int value2 = vhdl_evalfactor(more.factor);
			switch (more.mul_operator)
			{
				case MULOP_MULTIPLY:
					value *= value2;
					break;
				case MULOP_DIVIDE:
					value /= value2;
					break;
				case MULOP_MOD:
					value %= value2;
					break;
				case MULOP_REM:
					value -= (int)(value / value2) * value2;
					break;
				default:
					break;
			}
		}
		return value;
	}
	
	/**
	 * Method to get the value of a factor.
	 * @param factor pointer to a factor.
	 * @return value.
	 */
	private int vhdl_evalfactor(FACTOR factor)
	{
		if (factor == null) return 0;
		int value = vhdl_evalprimary(factor.primary);
		switch (factor.misc_operator)
		{
			case MISCOP_POWER:
				int value2 = vhdl_evalprimary(factor.primary2);
				while (value2-- != 0)
				{
					value += value;
				}
				break;
			case MISCOP_ABS:
				value = Math.abs(value);
				break;
			case MISCOP_NOT:
				if (value != 0) value = 0; else
					value = 1;
				break;
			default:
				break;
		}
		return value;
	}
	
	/**
	 * Method to evaluate the value of a primary and return.
	 * @param primary pointer to primary structure.
	 * @return evaluated value.
	 */
	private int vhdl_evalprimary(PRIMARY primary)
	{
		if (primary == null) return 0;
		int value = 0;
		switch (primary.type)
		{
			case PRIMARY_LITERAL:
				LITERAL literal = (LITERAL)primary.pointer;
				if (literal == null) break;
				switch (literal.type)
				{
					case LITERAL_NUMERIC:
						value = ((Integer)literal.pointer).intValue();
						break;
					case LITERAL_ENUMERATION:
					case LITERAL_STRING:
					case LITERAL_BIT_STRING:
					default:
						break;
				}
				break;
			case PRIMARY_NAME:
				value = vhdl_evalname((VNAME)primary.pointer);
				break;
			case PRIMARY_EXPRESSION:
				value = vhdl_evalexpression((EXPRESSION)primary.pointer);
				break;
			case PRIMARY_AGGREGATE:
			case PRIMARY_CONCATENATION:
			case PRIMARY_FUNCTION_CALL:
			case PRIMARY_TYPE_CONVERSION:
			case PRIMARY_QUALIFIED_EXPR:
			default:
				break;
		}
		return value;
	}
	
	/**
	 * Method to evaluate and return the value of a name.
	 * @param name pointer to name.
	 * @return value, 0 if no value.
	 */
	private int vhdl_evalname(VNAME name)
	{
		if (name == null) return 0;
		int value = 0;
		SYMBOLTREE symbol = vhdl_searchsymbol(vhdl_getnameident(name), vhdl_symbols);
		if (symbol == null)
		{
			vhdl_reporterrormsg(vhdl_getnametoken(name), "Symbol is undefined");
			return value;
		}
		if (symbol.type == SYMBOL_VARIABLE)
		{
			if (symbol.pointer instanceof Integer) value = ((Integer)symbol.pointer).intValue();
		} else if (symbol.type == SYMBOL_CONSTANT)
		{
			value = ((Integer)symbol.pointer).intValue();
		} else
		{
			vhdl_reporterrormsg(vhdl_getnametoken(name), "Cannot evaluate value of symbol");
			return value;
		}
		return value;
	}

	/**
	 * Method to do semantic analysis of signal declaration.
	 * @param signal pointer to signal declaration.
	 * @return pointer to new signals.
	 */
	private DBSIGNALS vhdl_semsignal_declare(SIGNALDECLARE signal)
	{
		DBSIGNALS signals = null;
		if (signal == null) return signals;
	
		// check for valid type
		String type = vhdl_getnameident(signal.subtype.type);
		SYMBOLTREE symbol = vhdl_searchsymbol(type, vhdl_symbols);
		if (symbol == null || symbol.type != SYMBOL_TYPE)
		{
			vhdl_reporterrormsg(vhdl_getnametoken(signal.subtype.type), "Bad type");
		}
	
		// check each signal in signal list for uniqueness
		for (IDENTLIST sig = signal.names; sig != null; sig = sig.next)
		{
			if (vhdl_searchsymbol((String)sig.identifier.pointer, vhdl_symbols) != null)
			{
				vhdl_reporterrormsg(sig.identifier, "Signal previously defined");
			} else
			{
				DBSIGNALS newsignal = new DBSIGNALS();
				newsignal.name = (String)sig.identifier.pointer;
				if (symbol != null)
				{
					newsignal.type = (DBLTYPE)symbol.pointer;
				} else
				{
					newsignal.type = null;
				}
				newsignal.next = signals;
				signals = newsignal;
				vhdl_addsymbol(newsignal.name, SYMBOL_SIGNAL, newsignal, vhdl_symbols);
			}
		}
	
		return signals;
	}

	/**
	 * Method to do semantic analysis of an interface declaration.
	 * @param interfacef pointer to interface parse structure.
	 * @return resultant database interface.
	 */
	private DBINTERFACE vhdl_seminterface(VINTERFACE interfacef)
	{
		DBINTERFACE dbinter = null;
		if (interfacef == null) return dbinter;
		if (vhdl_searchsymbol((String)interfacef.name.pointer, vhdl_gsymbols) != null)
		{
			vhdl_reporterrormsg(interfacef.name, "Entity previously defined");
		} else
		{
			dbinter = new DBINTERFACE();
			dbinter.name = (String)interfacef.name.pointer;
			dbinter.ports = null;
			dbinter.flags = 0;
			dbinter.bodies = null;
			dbinter.symbols = null;
			dbinter.next = null;
			vhdl_addsymbol(dbinter.name, SYMBOL_ENTITY, dbinter, vhdl_gsymbols);
			vhdl_symbols = vhdl_pushsymbols(vhdl_symbols);
			dbinter.ports = vhdl_semformal_port_list(interfacef.ports);
	
			// remove last symbol tree
			SYMBOLLIST endsymbol = vhdl_symbols;
			while (endsymbol.last.last != null)
			{
				endsymbol = endsymbol.last;
			}
			endsymbol.last = null;
			dbinter.symbols = vhdl_symbols;
		}
		return dbinter;
	}
	
	/**
	 * Method to check the semantic of the passed formal port list.
	 * @param port pointer to start of formal port list.
	 * @return pointer to database port list.
	 */
	private DBPORTLIST vhdl_semformal_port_list(FPORTLIST port)
	{
		DBPORTLIST dbports = null;
		DBPORTLIST endport = null;
	
		for (; port != null; port = port.next)
		{
			// check the mode of the port
			switch (port.mode)
			{
				case MODE_IN:
				case MODE_DOTOUT:
				case MODE_OUT:
				case MODE_INOUT:
				case MODE_LINKAGE:
					break;
				default:
					vhdl_reporterrormsg(port.names.identifier, "Unknown port mode");
					break;
			}
	
			// check the type
			String symName = vhdl_getnameident(port.type);
			SYMBOLTREE symbol = vhdl_searchsymbol(symName, vhdl_symbols);
			if (symbol == null || symbol.type != SYMBOL_TYPE)
			{
				vhdl_reporterrormsg(vhdl_getnametoken(port.type), "Unknown port name (" + symName + ")");
			}
	
			// check for uniqueness of port names
			for (IDENTLIST names = port.names; names != null; names = names.next)
			{
				if (vhdl_searchfsymbol((String)names.identifier.pointer, vhdl_symbols) != null)
				{
					vhdl_reporterrormsg(names.identifier, "Duplicate port name in port list");
				} else
				{
					// add to port list
					DBPORTLIST newport = new DBPORTLIST();
					newport.name = (String)names.identifier.pointer;
					newport.mode = port.mode;
					if (symbol != null)
					{
						newport.type = (DBLTYPE)symbol.pointer;
					} else
					{
						newport.type = null;
					}
					newport.flags = 0;
					newport.next = null;
					if (endport == null)
					{
						dbports = endport = newport;
					} else
					{
						endport.next = newport;
						endport = newport;
					}
					vhdl_addsymbol(newport.name, SYMBOL_FPORT, newport, vhdl_symbols);
				}
			}
		}
	
		return dbports;
	}
	
	/**
	 * Method to find a reference to a token given a pointer to a name.
	 * @param name pointer to name structure.
	 * @return pointer to token, null if not found.
	 */
	private TOKENLIST vhdl_getnametoken(VNAME name)
	{
		TOKENLIST token = null;
		if (name == null) return token;
		switch (name.type)
		{
			case NAME_SINGLE:
				SINGLENAME singl = (SINGLENAME)(name.pointer);
				switch (singl.type)
				{
					case SINGLENAME_SIMPLE:
						token = ((SIMPLENAME)singl.pointer).identifier;
						break;
					case SINGLENAME_SELECTED:
						break;
					case SINGLENAME_INDEXED:
						token = vhdl_getprefixtoken(((INDEXEDNAME)(singl.pointer)).prefix);
						break;
					case SINGLENAME_SLICE:
					default:
						break;
				}
				break;
			case NAME_CONCATENATE:
			case NAME_ATTRIBUTE:
			default:
				break;
		}
		return token;
	}
	
	/**
	 * Method to find a reference to a token given a pointer to a prefix.
	 * @param prefix pointer to prefix structure.
	 * @return pointer to token, null if not found.
	 */
	private TOKENLIST vhdl_getprefixtoken(PREFIX prefix)
	{
		TOKENLIST token = null;
		if (prefix == null) return token;
		switch (prefix.type)
		{
			case PREFIX_NAME:
				token = vhdl_getnametoken((VNAME)prefix.pointer);
				break;
			case PREFIX_FUNCTION_CALL:
			default:
				break;
		}
		return token;
	}

	/**
	 * Method to search the symbol list for a symbol of the passed value.
	 * Note that all symbol trees of the list are checked, from last to first.
	 * @param ident global name.
	 * @param sym_list pointer to last (current) symbol list.
	 * @return a pointer to the node, if not found, return null.
	 */
	private SYMBOLTREE vhdl_searchsymbol(String ident, SYMBOLLIST sym_list)
	{
		String lcIdent = ident.toLowerCase();
		for ( ; sym_list != null; sym_list = sym_list.last)
		{
			SYMBOLTREE node = (SYMBOLTREE)sym_list.sym.get(lcIdent);
			if (node != null) return node;
		}
		return null;
	}

	/**
	 * Method to search the symbol list for the first symbol of the passed value.
	 * Note that only the first symbol tree of the list is checked.
	 * @param ident global name.
	 * @param sym_list pointer to last (current) symbol list.
	 * @return a pointer to the node, if not found, return null.
	 */
	private SYMBOLTREE vhdl_searchfsymbol(String ident, SYMBOLLIST sym_list)
	{
		if (sym_list != null)
		{
			SYMBOLTREE node = (SYMBOLTREE)sym_list.sym.get(ident.toLowerCase());
			if (node != null) return node;
		}
		return null;
	}

	/**
	 * Method to get a name given a pointer to a name.
	 * @param name pointer to name structure.
	 * @return global name.
	 */
	private String vhdl_getnameident(VNAME name)
	{
		String itable = null;
		if (name == null) return itable;
		switch (name.type)
		{
			case NAME_SINGLE:
				SINGLENAME singl = (SINGLENAME)name.pointer;
				switch (singl.type)
				{
					case SINGLENAME_SIMPLE:
						itable = (String)((SIMPLENAME)singl.pointer).identifier.pointer;
						break;
					case SINGLENAME_INDEXED:
						itable = vhdl_getprefixident(((INDEXEDNAME)(singl.pointer)).prefix);
						break;
					case SINGLENAME_SELECTED:
					case SINGLENAME_SLICE:
					default:
						break;
				}
				break;
			case NAME_CONCATENATE:
			case NAME_ATTRIBUTE:
			default:
				break;
		}
		return itable;
	}
	
	/**
	 * Method to get a name given a pointer to a prefix.
	 * @param prefix pointer to prefix structure.
	 * @return string identifier.
	 */
	private String vhdl_getprefixident(PREFIX prefix)
	{
		String itable = null;
		if (prefix == null) return itable;
		switch (prefix.type)
		{
			case PREFIX_NAME:
				itable = vhdl_getnameident((VNAME)prefix.pointer);
				break;
			case PREFIX_FUNCTION_CALL:
			default:
				break;
		}
		return itable;
	}

	/**
	 * Method to create the default type symbol tree.
	 * @param symbols pointer to current symbol list.
	 */
	private void vhdl_createdefaulttype(SYMBOLLIST symbols)
	{
		// type BIT
		vhdl_identtable.add("BIT");
		vhdl_addsymbol("BIT", SYMBOL_TYPE, null, symbols);
	
		// type "std_logic"
		vhdl_identtable.add("std_logic");
		vhdl_addsymbol("std_logic", SYMBOL_TYPE, null, symbols);
	}
	
	/**
	 * Method to add a symbol to the symbol tree at the current symbol list.
	 * @param value pointer to identifier in namespace.
	 * @param type type of symbol.
	 * @param pointer generic pointer to symbol.
	 * @param sym_list pointer to symbol list.
	 * @return pointer to created symbol.
	 */
	private SYMBOLTREE vhdl_addsymbol(String value, int type, Object pointer, SYMBOLLIST sym_list)
	{
		SYMBOLTREE symbol = new SYMBOLTREE();
		symbol.value = value;
		symbol.type = type;
		symbol.pointer = pointer;
		sym_list.sym.put(value.toLowerCase(), symbol);
		return symbol;
	}
	
	/**
	 * Method to add a new symbol tree to the symbol list.
	 * @param old_sym_list pointer to old symbol list.
	 * @return the new symbol list.
	 */
	private SYMBOLLIST vhdl_pushsymbols(SYMBOLLIST old_sym_list)
	{
		SYMBOLLIST new_sym_list = new SYMBOLLIST();
		new_sym_list.sym = new HashMap();
		
		new_sym_list.last = old_sym_list;
	
		return new_sym_list;
	}

	/******************************** THE QUISC NETLIST GENERATOR ********************************/
	
	private static final int QNODE_SNAME	= 0;
	private static final int QNODE_INAME	= 1;
	private static final int QNODE_EXPORT	= 0x0001;
	private static final int QNODE_POWER	= 0x0002;
	private static final int QNODE_GROUND	= 0x0004;

	private static class QNODE
	{
		String	name;
		int		name_type;	/* type of name - simple or indexed */
		int		start, end;	/* range if array */
		int		size;		/* size of array if indexed */
		QPORT []table;		/* array of pointers if indexed */
		int		flags;		/* export flag */
		int		mode;		/* port mode if exported */
		QPORT	ports;		/* list of ports */
		QNODE	next;		/* next in list of nodes */
	};
	
	private static class QPORT
	{
		String	instname;	/* name of instance */
		String	portname;	/* name of port */
		boolean namealloc;	/* true if port name is allocated */
		QPORT	next;		/* next in port list */
	};

	/**
	 * Method to generate QUISC target output for the created parse tree.
	 * Assume parse tree is semantically correct.
	 * @return a list of strings that has the netlist.
	 */
	private List vhdl_genquisc()
	{
		List netlist = new ArrayList();

		// print file header
		netlist.add("!*************************************************");
		netlist.add("!  QUISC Command file");
		netlist.add("!");
		if (User.isIncludeDateAndVersionInOutput())
			netlist.add("!  File Creation:    " + TextUtils.formatDate(new Date()));
		netlist.add("!-------------------------------------------------");
		netlist.add("");
	
		// determine top level cell
		DBINTERFACE top_interface = vhdl_findtopinterface(vhdl_units);
		if (top_interface == null)
			System.out.println("ERROR - Cannot find top interface."); else
		{
			// clear written flag on all entities
			for (DBINTERFACE interfacef = vhdl_units.interfaces; interfacef != null;
				interfacef = interfacef.next) interfacef.flags &= ~ENTITY_WRITTEN;
			vhdl_genquisc_interface(top_interface, netlist);
		}
	
		// print closing line of output file
		netlist.add("!********* End of command file *******************");
	
		// scan unresolved references for reality inside of Electric
		Library celllib = SilComp.getCellLib();
		int total = 0;
		for (UNRESLIST ulist = vhdl_unresolved_list; ulist != null; ulist = ulist.next)
		{
			// see if this is a reference to a cell in the current library
			boolean found = false;
			for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
			{
				Cell np = (Cell)it.next();
				StringBuffer sb = new StringBuffer();
				String name = np.getName();
				for(int i=0; i<name.length();  i++)
				{
					char chr = name.charAt(i);
					if (Character.isLetterOrDigit(chr)) sb.append(chr); else
						sb.append('_');
				}
				if (ulist.interfacef.equalsIgnoreCase(sb.toString())) { found = true;   break; }
			}
			if (!found && celllib != null)
			{
				for(Iterator it = celllib.getCells(); it.hasNext(); )
				{
					Cell np = (Cell)it.next();
					StringBuffer sb = new StringBuffer();
					String name = np.getName();
					for(int i=0; i<name.length();  i++)
					{
						char chr = name.charAt(i);
						if (Character.isLetterOrDigit(chr)) sb.append(chr); else
							sb.append('_');
					}
					if (ulist.interfacef.equalsIgnoreCase(sb.toString())) { found = true;   break; }
				}
			}
			if (found)
			{
				ulist.numref = 0;
				continue;
			}
			total++;
		}
	
		// print unresolved reference list
		if (total > 0)
		{
			System.out.println("*****  UNRESOLVED REFERENCES *****");
			for (UNRESLIST ulist = vhdl_unresolved_list; ulist != null; ulist = ulist.next)
				if (ulist.numref > 0)
					System.out.println(ulist.interfacef + ", " + ulist.numref + " time(s)");
		}
		return netlist;
	}

	/**
	 * Method to find the top interface in the database.
	 * The top interface is defined as the interface is called by no other architectural bodies.
	 * @param units pointer to database design units.
	 * @return pointer to top interface.
	 */
	private DBINTERFACE vhdl_findtopinterface(DBUNITS units)
	{
		/* clear flags of all interfaces in database */
		for (DBINTERFACE interfacef = units.interfaces; interfacef != null; interfacef = interfacef.next)
			interfacef.flags &= ~TOP_ENTITY_FLAG;

		/* go through the list of bodies and flag any interfaces */
		for (DBBODY body = units.bodies; body != null; body = body.next)
		{
			/* go through component list */
			if (body.declare == null) continue;
			for (DBCOMPONENTS compo = body.declare.components; compo != null; compo = compo.next)
			{
				SYMBOLTREE symbol = vhdl_searchsymbol(compo.name, vhdl_gsymbols);
				if (symbol != null && symbol.pointer != null)
				{
					((DBINTERFACE)(symbol.pointer)).flags |= TOP_ENTITY_FLAG;
				}
			}
		}

		/* find interface with the flag bit not set */
		DBINTERFACE interfacef;
		for (interfacef = units.interfaces; interfacef != null; interfacef = interfacef.next)
		{
			if ((interfacef.flags & TOP_ENTITY_FLAG) == 0) break;
		}
		return interfacef;
	}

	/**
	 * Method to recursively generate the QUISC description for the specified model.
	 * Works by first generating the lowest interface instantiation and working back to the top (i.e. bottom up).
	 * @param interfacef pointer to interface.
	 * @param netlist the List of strings to create.
	 */
	private void vhdl_genquisc_interface(DBINTERFACE interfacef, List netlist)
	{
		// go through interface's architectural body and call generate interface
		// for any interface called by an instance which has not been already
		// generated
	
		// check written flag
		if ((interfacef.flags & ENTITY_WRITTEN) != 0) return;
	
		// set written flag
		interfacef.flags |= ENTITY_WRITTEN;
	
		// check all instants of corresponding architectural body
		// and write if non-primitive interfaces
		if (interfacef.bodies != null && interfacef.bodies.statements != null)
		{
			for (DBINSTANCE inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
			{
				SYMBOLTREE symbol = vhdl_searchsymbol(inst.compo.name, vhdl_gsymbols);
				if (symbol == null || symbol.pointer == null)
				{
					if (vhdl_externentities)
					{
						if (vhdl_warnflag)
							System.out.println("WARNING - interface " + inst.compo.name + " not found, assumed external.");

						// add to unresolved list
						UNRESLIST ulist;
						for (ulist = vhdl_unresolved_list; ulist != null; ulist = ulist.next)
						{
							if (ulist.interfacef == inst.compo.name) break;
						}
						if (ulist != null) ulist.numref++; else
						{
							ulist = new UNRESLIST();
							ulist.interfacef = inst.compo.name;
							ulist.numref = 1;
							ulist.next = vhdl_unresolved_list;
							vhdl_unresolved_list = ulist;
						}
					} else
						System.out.println("ERROR - interface " + inst.compo.name + "not found.");
					continue;
				} else vhdl_genquisc_interface((DBINTERFACE)symbol.pointer, netlist);
			}
		}
	
		// write this entity
		netlist.add("create cell " + interfacef.name);
	
		// write out instances as components
		if (interfacef.bodies != null && interfacef.bodies.statements != null)
		{
			for (DBINSTANCE inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
				netlist.add("create instance " + inst.name + " " + inst.compo.name);
		}
	
		// create export list
		QNODE qnodes = null;
		QNODE lastnode = null;
		for (DBPORTLIST fport = interfacef.ports; fport != null; fport = fport.next)
		{
			if (fport.type == null || fport.type.type == DBTYPE_SINGLE)
			{
				QNODE newnode = new QNODE();
				newnode.name = fport.name;
				newnode.name_type = QNODE_SNAME;
				newnode.size = 0;
				newnode.start = 0;
				newnode.end = 0;
				newnode.table = null;
				newnode.flags = QNODE_EXPORT;
				newnode.mode = fport.mode;
				newnode.ports = null;
				newnode.next = null;
				if (lastnode == null) qnodes = lastnode = newnode; else
				{
					lastnode.next = newnode;
					lastnode = newnode;
				}
			} else
			{
				QNODE newnode = new QNODE();
				newnode.name = fport.name;
				newnode.name_type = QNODE_INAME;
				newnode.flags = QNODE_EXPORT;
				newnode.mode = fport.mode;
				newnode.ports = null;
				newnode.next = null;
				if (lastnode == null) qnodes = lastnode = newnode; else
				{
					lastnode.next = newnode;
					lastnode = newnode;
				}
				DBINDEXRANGE irange = (DBINDEXRANGE)fport.type.pointer;
				DBDISCRETERANGE drange = irange.drange;
				newnode.start = drange.start;
				newnode.end = drange.end;
				int size = 1;
				if (drange.start > drange.end)
				{
					size = drange.start - drange.end + 1;
				} else if (drange.start < drange.end)
				{
					size = drange.end - drange.start + 1;
				}
				newnode.size = size;
				newnode.table = new QPORT[size];
				for (int i = 0; i < size; i++) newnode.table[i] = null;
			}
		}
	
		// add local signals
		if (interfacef.bodies != null && interfacef.bodies.declare != null)
		{
			for (DBSIGNALS signal = interfacef.bodies.declare.bodysignals; signal != null; signal = signal.next)
			{
				if (signal.type == null || signal.type.type == DBTYPE_SINGLE)
				{
					QNODE newnode = new QNODE();
					newnode.name = signal.name;
					newnode.name_type = QNODE_SNAME;
					newnode.size = 0;
					newnode.start = 0;
					newnode.end = 0;
					newnode.table = null;
					if (signal.name.equalsIgnoreCase("power"))
					{
						newnode.flags = QNODE_POWER;
					} else if (signal.name.equalsIgnoreCase("ground"))
					{
						newnode.flags = QNODE_GROUND;
					} else
					{
						newnode.flags = 0;
					}
					newnode.mode = 0;
					newnode.ports = null;
					newnode.next = null;
					if (lastnode == null)
					{
						qnodes = lastnode = newnode;
					} else
					{
						lastnode.next = newnode;
						lastnode = newnode;
					}
				} else
				{
					QNODE newnode = new QNODE();
					newnode.name = signal.name;
					newnode.name_type = QNODE_INAME;
					newnode.flags = 0;
					newnode.mode = 0;
					newnode.ports = null;
					newnode.next = null;
					if (lastnode == null)
					{
						qnodes = lastnode = newnode;
					} else
					{
						lastnode.next = newnode;
						lastnode = newnode;
					}
					DBINDEXRANGE irange = (DBINDEXRANGE)signal.type.pointer;
					DBDISCRETERANGE drange = irange.drange;
					newnode.start = drange.start;
					newnode.end = drange.end;
					int size = 1;
					if (drange.start > drange.end)
					{
						size = drange.start - drange.end + 1;
					} else if (drange.start < drange.end)
					{
						size = drange.end - drange.start + 1;
					}
					newnode.size = size;
					newnode.table = new QPORT[size];
					for (int i = 0; i < size; i++) newnode.table[i] = null;
				}
			}
		}
	
		// write out connects
		if (interfacef.bodies != null && interfacef.bodies.statements != null)
		{
			for (DBINSTANCE inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
			{
				// check all instance ports for connections
				for (DBAPORTLIST aport = inst.ports; aport != null; aport = aport.next)
				{
					if (aport.name == null) continue;
	
					// get names of all members of actual port
					switch (aport.name.type)
					{
						case DBNAME_IDENTIFIER:
							vhdl_addidentaport(aport.name, aport.port, 0, inst, qnodes);
							break;
						case DBNAME_INDEXED:
							vhdl_addindexedaport(aport.name, aport.port, 0, inst, qnodes);
							break;
						case DBNAME_CONCATENATED:
							int offset = 0;
							for (DBNAMELIST cat = (DBNAMELIST)aport.name.pointer; cat != null; cat = cat.next)
							{
								if (cat.name.type == DBNAME_IDENTIFIER)
								{
									vhdl_addidentaport(cat.name, aport.port, offset, inst, qnodes);
								} else
								{
									vhdl_addindexedaport(cat.name, aport.port, offset, inst, qnodes);
								}
								offset += vhdl_querysize(cat.name);
							}
							break;
						default:
							System.out.println("ERROR - unknown name type on actual port.");
							break;
					}
				}
			}
		}
	
		// print out connections
		for (QNODE newnode = qnodes; newnode != null; newnode = newnode.next)
		{
			if (newnode.name_type == QNODE_SNAME)
			{
				QPORT qport = newnode.ports;
				if (qport != null)
				{
					for (QPORT qport2 = qport.next; qport2 != null; qport2 = qport2.next)
					{
						netlist.add("connect " + qport.instname + " " + qport.portname + " " + qport2.instname + " " + qport2.portname);
					}
					if ((newnode.flags & QNODE_POWER) != 0)
					{
						netlist.add("connect " + qport.instname + " " + qport.portname + " power");
					}
					if ((newnode.flags & QNODE_GROUND) != 0)
					{
						netlist.add("connect " + qport.instname + " " + qport.portname + " ground");
					}
				}
			} else
			{
				for (int i = 0; i < newnode.size; i++)
				{
					QPORT qport = newnode.table[i];
					if (qport != null)
					{
						for (QPORT qport2 = qport.next; qport2 != null; qport2 = qport2.next)
						{
							netlist.add("connect " + qport.instname + " " + qport.portname + " " + qport2.instname + " " + qport2.portname);
						}
					}
				}
			}
		}
	
		// print out exports
		for (QNODE newnode = qnodes; newnode != null; newnode = newnode.next)
		{
			if ((newnode.flags & QNODE_EXPORT) != 0)
			{
				if (newnode.name_type == QNODE_SNAME)
				{
					QPORT qport = newnode.ports;
					if (qport != null)
					{
						String inout = "";
						switch (newnode.mode)
						{
							case DBMODE_IN:  inout = " input";    break;
							case DBMODE_OUT: inout = " output";   break;
						}
						netlist.add("export " + qport.instname + " " + qport.portname + " " + newnode.name + inout);
					} else
					{
						System.out.println("ERROR - no export for " + newnode.name);
					}
				} else
				{
					for (int i = 0; i < newnode.size; i++)
					{
						int indexc = 0;
						if (newnode.start > newnode.end)
						{
							indexc = newnode.start - i;
						} else
						{
							indexc = newnode.start + i;
						}
						QPORT qport = newnode.table[i];
						if (qport != null)
						{
							String inout = "";
							switch (newnode.mode)
							{
								case DBMODE_IN:  inout = " input";    break;
								case DBMODE_OUT: inout = " output";   break;
							}
							netlist.add("export " + qport.instname + " " + qport.portname + " " + newnode.name + "[" + indexc + "]" + inout);
						} else
						{
							System.out.println("ERROR - no export for " + newnode.name + "[" + indexc + "]");
						}
					}
				}
			}
		}
	
		// extract entity
		netlist.add("extract");
	
		// print out non-exported node name assignments
		for (QNODE newnode = qnodes; newnode != null; newnode = newnode.next)
		{
			if ((newnode.flags & QNODE_EXPORT) == 0)
			{
				if (newnode.name_type == QNODE_SNAME)
				{
					QPORT qport = newnode.ports;
					if (qport != null)
					{
						netlist.add("set node-name " + qport.instname + " " + qport.portname + " " + newnode.name);
					}
				} else
				{
					for (int i = 0; i < newnode.size; i++)
					{
						int indexc = 0;
						if (newnode.start > newnode.end)
						{
							indexc = newnode.start - i;
						} else
						{
							indexc = newnode.start + i;
						}
						QPORT qport = newnode.table[i];
						if (qport != null)
						{
							netlist.add("set node-name " + qport.instname + " " + qport.portname + " " + newnode.name + "[" + indexc + "]");
						}
					}
				}
			}
		}
	
		netlist.add("");
	}
	
	/**
	 * Method to get the size (in number of elements) of the passed name.
	 * @param name pointer to the name.
	 * @return number of elements, 0 default.
	 */
	private int vhdl_querysize(DBNAME name)
	{
		int size = 0;
		if (name != null)
		{
			switch (name.type)
			{
				case DBNAME_IDENTIFIER:
					if (name.dbtype != null)
					{
						switch (name.dbtype.type)
						{
							case DBTYPE_SINGLE:
								size = 1;
								break;
							case DBTYPE_ARRAY:
								DBINDEXRANGE irange = (DBINDEXRANGE)name.dbtype.pointer;
								if (irange != null)
								{
									DBDISCRETERANGE drange = irange.drange;
									if (drange != null)
									{
										if (drange.start > drange.end)
										{
											size = drange.start - drange.end;
										} else
										{
											size = drange.end - drange.start;
										}
										size++;
									}
								}
								break;
							default:
								break;
						}
					} else
					{
						size = 1;
					}
					break;
				case DBNAME_INDEXED:
					size = 1;
					break;
				default:
					break;
			}
		}
		return size;
	}
	
	/**
	 * Method to add the actual port of identifier name type to the node list.
	 * @param name pointer to name.
	 * @param port pointer to port on component.
	 * @param offset offset in bits if of array type.
	 * @param inst pointer to instance of component.
	 * @param qnodes address of start of node list.
	 */
	private void vhdl_addidentaport(DBNAME name, DBPORTLIST port, int offset, DBINSTANCE inst, QNODE qnodes)
	{
		if (name.dbtype != null && name.dbtype.type == DBTYPE_ARRAY)
		{
			DBINDEXRANGE irange = (DBINDEXRANGE)name.dbtype.pointer;
			if (irange != null)
			{
				DBDISCRETERANGE drange = irange.drange;
				if (drange != null)
				{
					int delta = 0;
					if (drange.start > drange.end)
					{
						delta = -1;
					} else if (drange.start < drange.end)
					{
						delta = 1;
					}
					int i = drange.start - delta;
					int offset2 = 0;
					do
					{
						i += delta;
						QPORT newport = vhdl_createqport(inst.name, port, offset + offset2);
						vhdl_addporttonode(newport, name.name, QNODE_INAME, i, qnodes);
						offset2++;
					} while (i != drange.end);
				}
			}
		} else
		{
			QPORT newport = vhdl_createqport(inst.name, port, offset);
			vhdl_addporttonode(newport, name.name, QNODE_SNAME, 0, qnodes);
		}
	}
	
	/**
	 * Method to add the actual port of indexed name type to the node list.
	 * @param name pointer to name.
	 * @param port pointer to port on component.
	 * @param offset offset in bits if of array type.
	 * @param inst pointer to instance of component.
	 * @param qnodes address of start of node list.
	 */
	private void vhdl_addindexedaport(DBNAME name, DBPORTLIST port, int offset, DBINSTANCE inst, QNODE qnodes)
	{
		QPORT newport = vhdl_createqport(inst.name, port, offset);
		int indexc = ((DBEXPRLIST)name.pointer).value;
		vhdl_addporttonode(newport, name.name, QNODE_INAME, indexc, qnodes);
	}
	

	/**
	 * Method to create a qport for the indicated port.
	 * @param iname name of instance.
	 * @param port pointer to port on component.
	 * @param offset offset if array.
	 * @return address of created QPORT.
	 */
	private QPORT vhdl_createqport(String iname, DBPORTLIST port, int offset)
	{
		QPORT newport = new QPORT();
		newport.instname = iname;
		newport.next = null;
		if (port.type != null && port.type.type == DBTYPE_ARRAY)
		{
			newport.portname = port.name + "[" + offset + "]";
			newport.namealloc = true;
		} else
		{
			newport.portname = port.name;
			newport.namealloc = false;
		}
	
		return newport;
	}
	
	/**
	 * Method to add the port to the node list.
	 * @param port port to add.
	 * @param ident name of node to add to.
	 * @param type if simple or indexed.
	 * @param indexc index if arrayed.
	 * @param qnodes address of pointer to start of list.
	 */
	private void vhdl_addporttonode(QPORT port, String ident, int type, int indexc, QNODE qnodes)
	{
		for (QNODE node = qnodes; node != null; node = node.next)
		{
			if (node.name.equalsIgnoreCase(ident))
			{
				if (node.name_type == type)
				{
					if (type == QNODE_SNAME)
					{
						port.next = node.ports;
						node.ports = port;
						return;
					} else
					{
						int tindex;
						if (node.start > node.end)
						{
							tindex = node.start - indexc;
						} else
						{
							tindex = indexc - node.start;
						}
						if (tindex >= 0 && tindex < node.size)
						{
							port.next = node.table[tindex];
							node.table[tindex] = port;
							return;
						}
					}
				}
			}
		}
		System.out.println("WARNING node " + ident + " not found");
	}
}
