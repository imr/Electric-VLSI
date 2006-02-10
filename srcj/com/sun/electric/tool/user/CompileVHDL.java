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
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
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

	/********** Miscellaneous Constants *********************************/

	/** enternal entities flag */		private static final boolean EXTERNALENTITIES = true;
	/** warning flag, TRUE warn */		private static final boolean WARNFLAG = false;
	/** flag the entity as called */	private static final int     TOP_ENTITY_FLAG	= 0x0001;
	/** flag the entity as written */	private static final int     ENTITY_WRITTEN		= 0x0002;

	/********** Keyword Structures *****************************************/

	private static class VKeyword
	{
		/** string defining keyword */	String	name;
		/** number of keyword */		int		num;

		VKeyword(String name, int num) { this.name = name;   this.num = num; }
	};

	/********** Token Structures *****************************************/

	private class TokenList
	{
		/** token number */								int	token;
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
		 /** next in list */							TokenList next;
		 /** previous in list */						TokenList last;

		TokenList(int token, Object pointer, int lineNum, boolean space)
		{
			this.token = token;
			this.pointer = pointer;
			this.lineNum = lineNum;
			this.space = true;
			this.next = null;
			this.last = tListEnd;
			if (tListEnd == null)
			{
				tListStart = tListEnd = this;
			} else
			{
				tListEnd.space = space;
				tListEnd.next = this;
				tListEnd = this;
			}
		}

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

	private static class SymbolTree
	{
		/** identifier */				String			value;
		/** type of item */				int				type;
		/** pointer to item */			Object			pointer;
		/** flag for deallocation */	int             seen;
	};

	private static class SymbolList
	{
		/** the symbol table map */		HashMap<String,SymbolTree> sym;
		/** previous in stack */		SymbolList		           last;
		/** next in list */				SymbolList		           next;
	};

	/********** Unresolved Reference List **********************************/

	private static class UnResList
	{
		/** name of reference */		String			interfacef;
		/** number of references */		int				numRef;
		/** next in list */				UnResList		next;
	};

	/***********************************************************************/

	private static class DBUnits
	{
		/** list of interfaces */		DBInterface		interfaces;
		/** list of bodies */			DBBody			bodies;
	};

	private static class DBPackage
	{
		/** name of package */			String			name;
		/** root of symbol tree */		SymbolList		root;
	};

	private static class DBInterface
	{
		/** name of interface */		String			name;
		/** list of ports */			DBPortList		ports;
		/** interface declarations */	Object			interfacef;
		/** for later code gen */		int				flags;
		/** associated bodies */		DBBody			bodies;
		/** local symbols */			SymbolList		symbols;
		/** next interface */			DBInterface		next;
	};

	private static final int DBMODE_IN			= 1;
	private static final int DBMODE_OUT			= 2;
	private static final int DBMODE_DOTOUT		= 3;
	private static final int DBMODE_INOUT		= 4;
	private static final int DBMODE_LINKAGE		= 5;

	private static class DBPortList
	{
		/** name of port */				String			name;
		/** mode of port */				int				mode;
		/** type of port */				DBLType			type;
		/** general flags */			int				flags;
		/** next in port list */		DBPortList		next;
	};

	private static final int DBTYPE_SINGLE		= 1;
	private static final int DBTYPE_ARRAY		= 2;

	private static class DBLType
	{
		/** name of type */				String			name;
		/** type of type */				int				type;
		/** pointer to info */			Object			pointer;
		/** possible subtype */			DBLType			subType;
	};

	/********** Bodies *****************************************************/

	private static class DBBody
	{
		/** name of body: identifier */	String			name;
		/** parent entity of body */	String			entity;
		/** declarations */				DBBodyDelcare	declare;
		/** statements in body */		DBStatements	statements;
		/** pointer to parent */		DBInterface		parent;
		/** bodies of same parent */	DBBody			sameParent;
		/** next body */				DBBody			next;
	};

	private static class DBBodyDelcare
	{
		/** components */				DBComponents	components;
		/** signals */					DBSignals		bodySignals;
	};

	private static class DBComponents
	{
		/** name of component */		String			name;
		/** list of ports */			DBPortList		ports;
		/** next component */			DBComponents	next;
	};

	private static class DBSignals
	{
		/** name of signal */			String			name;
		/** type of signal */			DBLType			type;
		/** next signal */				DBSignals		next;
	};

	/********** Architectural Statements ***********************************/

	private static class DBStatements
	{
		DBInstance		instances;
	};

	private static class DBInstance
	{
		/** identifier */				String			name;
		/** component */				DBComponents	compo;
		/** ports on instance */		DBAPortList		ports;
		/** next instance in list */	DBInstance		next;
	};

	private static class DBAPortList
	{
		/** name of port */				DBName			name;
		/** pointer to port on comp */	DBPortList		port;
		/** flags for processing */		int				flags;
		/** next in list */				DBAPortList		next;
	};

	/********** Names ******************************************************/

	private static final int DBNAME_IDENTIFIER		= 1;
	private static final int DBNAME_INDEXED			= 2;
	private static final int DBNAME_CONCATENATED	= 3;

	private static class DBName
	{
		/** name of name */				String			name;
		/** type of name */				int				type;
		/** null if identifier
		 * DBExprList if indexed
		 * DBNameList if concatenated */Object			pointer;
		/** pointer to type */			DBLType			dbType;
	};

	private static class DBExprList
	{
		/** value */					int				value;
		/** next in list */				DBExprList		next;
	};

	private static class DBDiscreteRange
	{
		/** start of range */			int				start;
		/** end of range */				int				end;
	};

	private static class DBIndexRange
	{
		/** discrete range */			DBDiscreteRange	dRange;
		/** next in list */				DBIndexRange	next;
	};

	private static class DBNameList
	{
		/** name in list */				DBName			name;
		/** next in list */				DBNameList		next;
	};

	/******** Parser Constants and Structures ******************************/

	private static final int NOUNIT			= 0;
	private static final int UNIT_INTERFACE	= 1;
	private static final int UNIT_FUNCTION	= 2;
	private static final int UNIT_PACKAGE	= 3;
	private static final int UNIT_BODY		= 4;
	private static final int UNIT_USE		= 6;

	private static class PTree
	{
		/** type of entity */			int				type;
		/** pointer to design unit */	Object			pointer;
		/** pointer to next */			PTree			next;
	};

	/********** Packages ***************************************************/

	private static class Package
	{
		/** package name */				TokenList		name;
		/** package declare part */		PackagedPart	declare;
		/** package declare items */	List			packagedParts;
	};

	private static class PackagedPart
	{
		/** package declare item */		BasicDeclare	item;
		/** pointer to next */			PackagedPart	next;
	};

	private static class Use
	{
		/** unit */						TokenList		unit;
		/** next in list */				Use				next;
	};

	/********** Interfaces *************************************************/

	private static class VInterface
	{
		/** name of entity */			TokenList		name;
		/** list of ports */			FPortList		ports;
		/** interface declarations */	Object			interfacef;
	};

	private static final int MODE_IN			= 1;
	private static final int MODE_OUT			= 2;
	private static final int MODE_DOTOUT		= 3;
	private static final int MODE_INOUT			= 4;
	private static final int MODE_LINKAGE		= 5;

	private static class FPortList
	{
		/** names of port */			IdentList		names;
		/** mode of port */				int				mode;
		/** type of port */				VName			type;
		/** next in port list */		FPortList		next;
	};

	private static class IdentList
	{
		/** identifier */				TokenList		identifier;
		/** next in list */				IdentList		next;
	};

	/********** Bodies *****************************************************/

	private static final int BODY_BEHAVIORAL	= 1;
	private static final int BODY_ARCHITECTURAL	= 2;

	private static class Body
	{
		/** name of body: identifier */	TokenList		name;
		/** parent entity of body */	SimpleName		entity;
		/** body declarations */		BodyDeclare		bodyDeclare;
		/** statements in body */		Statements		statements;
	};

	private static final int BODYDECLARE_BASIC		= 1;
	private static final int BODYDECLARE_COMPONENT	= 2;
	private static final int BODYDECLARE_RESOLUTION	= 3;
	private static final int BODYDECLARE_LOCAL		= 4;

	private static class BodyDeclare
	{
		/** type of declaration */		int				type;
		/** pointer to part tree */		Object			pointer;
		/** next in list */				BodyDeclare		next;
	};

	/********** Basic Declarations *****************************************/

	private static final int NOBASICDECLARE				= 0;
	private static final int BASICDECLARE_OBJECT		= 1;
	private static final int BASICDECLARE_TYPE			= 2;
	private static final int BASICDECLARE_SUBTYPE		= 3;
	private static final int BASICDECLARE_CONVERSION	= 4;
	private static final int BASICDECLARE_ATTRIBUTE		= 5;
	private static final int BASICDECLARE_ATT_SPEC		= 6;

	private static class BasicDeclare
	{
		/** type of basic declare */	int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	private static final int NOOBJECTDECLARE			= 0;
	private static final int OBJECTDECLARE_CONSTANT		= 1;
	private static final int OBJECTDECLARE_SIGNAL		= 2;
	private static final int OBJECTDECLARE_VARIABLE		= 3;
	private static final int OBJECTDECLARE_ALIAS		= 4;

	private static class ObjectDeclare
	{
		/** type of object declare */	int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	private static class SignalDeclare
	{
		/** list of identifiers */		IdentList		names;
		/** subtype indicator */		SubTypeInd		subType;
	};

	private static class VComponent
	{
		/** name of component */		TokenList		name;
		/** ports of component */		FPortList		ports;
	};

	private static class ConstantDeclare
	{
		/** name of constant */			TokenList		identifier;
		/** subtype indicator */		SubTypeInd		subType;
		/** expression */				Expression		expression;
	};

	/********** Types ******************************************************/

	private static class SubTypeInd
	{
		/** type of subtype */			VName			type;
	};

	private static final int TYPE_SCALAR		= 1;
	private static final int TYPE_COMPOSITE		= 2;

	private static class Type
	{
		/** name of type */				TokenList		identifier;
		/** type definition */			int				type;
		/** pointer to type */			Object			pointer;
	};

	private static final int COMPOSITE_ARRAY	= 1;
	private static final int COMPOSITE_RECORD	= 2;

	private static class Composite
	{
		/** type of composite */		int				type;
		/** pointer to composite */		Object			pointer;
	};

	private static final int ARRAY_UNCONSTRAINED	= 1;
	private static final int ARRAY_CONSTRAINED		= 2;

	private static class Array
	{
		/** (un)constrained array */	int				type;
		/** pointer to array */			Object			pointer;
	};

	private static class Constrained
	{
		/** index constraint */			IndexConstraint	constraint;
		/** subtype indication */		SubTypeInd		subType;
	};

	private static class IndexConstraint
	{
		/** discrete range */			DiscreteRange	discrete;
		/** possible more */			IndexConstraint	next;
	};

	/********** Architectural Statements ***********************************/

	private static final int NOARCHSTATE			= 0;
	private static final int ARCHSTATE_GENERATE		= 1;
	private static final int ARCHSTATE_SIG_ASSIGN	= 2;
	private static final int ARCHSTATE_IF			= 3;
	private static final int ARCHSTATE_CASE			= 4;
	private static final int ARCHSTATE_INSTANCE		= 5;
	private static final int ARCHSTATE_NULL			= 6;

	private static class Statements
	{
		/** type of statement */		int				type;
		/** pointer to parse tree */	Object			pointer;
		/** pointer to next */			Statements		next;
	};

	private static class VInstance
	{
		/** optional identifier */		TokenList		name;
		/** entity of instance */		SimpleName		entity;
		/** ports on instance */		APortList		ports;
	};

	private static final int APORTLIST_NAME			= 1;
	private static final int APORTLIST_TYPE_NAME	= 2;
	private static final int APORTLIST_EXPRESSION	= 3;

	private static class APortList
	{
		/** type of actual port */		int				type;
		/** pointer to parse tree */	Object			pointer;
		/** next in list */				APortList		next;
	};

	private static class Generate
	{
		/** optional label */			TokenList		label;
		/** generate scheme */			GenScheme		genScheme;
		/** statements */				Statements		statements;
	};

	private static final int GENSCHEME_FOR		= 0;
	private static final int GENSCHEME_IF		= 1;

	private static class GenScheme
	{
		/** scheme (for or if) */		int				scheme;
		/** if FOR scheme */			TokenList		identifier;
		/** if FOR scheme */			DiscreteRange	range;
		/** if IF scheme */				Expression		condition;
	};

	/********** Names ******************************************************/

	private static final int NONAME				= 0;
	private static final int NAME_SINGLE		= 1;
	private static final int NAME_CONCATENATE	= 2;
	private static final int NAME_ATTRIBUTE		= 3;

	private static class VName
	{
		/** type of name */				int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	private static final int NOSINGLENAME			= 0;
	private static final int SINGLENAME_SIMPLE		= 1;
	private static final int SINGLENAME_SELECTED	= 2;
	private static final int SINGLENAME_INDEXED		= 3;
	private static final int SINGLENAME_SLICE		= 4;

	private static class SingleName
	{
		/** type of simple name */		int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	private static class SimpleName
	{
		/** identifier */				TokenList		identifier;
	};

	private static final int PREFIX_NAME			= 1;
	private static final int PREFIX_FUNCTION_CALL	= 2;

	private static class Prefix
	{
		/** type of prefix */			int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	private static class IndexedName
	{
		/** prefix */					Prefix			prefix;
		/** expression list */			ExprList		exprList;
	};

	private static class ExprList
	{
		/** expression */				Expression		expression;
		/** next in list */				ExprList		next;
	};

	private static final int DISCRETERANGE_SUBTYPE	= 1;
	private static final int DISCRETERANGE_RANGE	= 2;

	private static class DiscreteRange
	{
		/** type of discrete range */	int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	private static final int RANGE_ATTRIBUTE	= 1;
	private static final int RANGE_SIMPLE_EXPR	= 2;

	private static class Range
	{
		/** type of range */			int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	private static class RangeSimple
	{
		/** start of range */			SimpleExpr		start;
		/** end of range */				SimpleExpr		end;
	};

	private static class ConcatenatedName
	{
		/** single name */				SingleName		name;
		/** next in list */				ConcatenatedName next;
	};

	/********** Expressions ************************************************/

	private static final int NOLOGOP		= 0;
	private static final int LOGOP_AND		= 1;
	private static final int LOGOP_OR		= 2;
	private static final int LOGOP_NAND		= 3;
	private static final int LOGOP_NOR		= 4;
	private static final int LOGOP_XOR		= 5;

	private static class Expression
	{
		/** first relation */			Relation		relation;
		/** more relations */			MRelations		next;
	};

	private static final int NORELOP		= 0;
	private static final int RELOP_EQ		= 1;
	private static final int RELOP_NE		= 2;
	private static final int RELOP_LT		= 3;
	private static final int RELOP_LE		= 4;
	private static final int RELOP_GT		= 5;
	private static final int RELOP_GE		= 6;

	private static class Relation
	{
		/** simple expression */		SimpleExpr		simpleExpr;
		/** possible operator */		int				relOperator;
		/** possible expression */		SimpleExpr		simpleExpr2;
	};

	private static class MRelations
	{
		/** logical operator */			int				logOperator;
		/** relation */					Relation		relation;
		/** more relations */			MRelations		next;
	};

	private static final int NOADDOP			= 0;
	private static final int ADDOP_ADD			= 1;
	private static final int ADDOP_SUBTRACT		= 2;

	private static class SimpleExpr
	{
		/** sign (1 or  -1) */			int				sign;
		/** first term */				Term			term;
		/** additional terms */			MTerms			next;
	};

	private static final int NOMULOP			= 0;
	private static final int MULOP_MULTIPLY		= 1;
	private static final int MULOP_DIVIDE		= 2;
	private static final int MULOP_MOD			= 3;
	private static final int MULOP_REM			= 4;

	private static class Term
	{
		/** first factor */				Factor			factor;
		/** additional factors */		MFactors		next;
	};

	private static class MTerms
	{
		/** add operator */				int				addOperator;
		/** next term */				Term			term;
		/** any more terms */			MTerms			next;
	};

	private static final int NOMISCOP			= 0;
	private static final int MISCOP_POWER		= 1;
	private static final int MISCOP_ABS			= 2;
	private static final int MISCOP_NOT			= 3;

	private static class Factor
	{
		/** first primary */			Primary			primary;
		/** possible operator */		int				miscOperator;
		/** possible primary */			Primary			primary2;
	};

	private static class MFactors
	{
		/** operator */					int				mulOperator;
		/** next factor */				Factor			factor;
		/** possible more factors */	MFactors		next;
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

	private static class Primary
	{
		/** type of primary */			int				type;
		/** pointer to primary */		Object			pointer;
	};

	private static final int NOLITERAL				= 0;
	private static final int LITERAL_NUMERIC		= 1;
	private static final int LITERAL_ENUMERATION	= 2;
	private static final int LITERAL_STRING			= 3;
	private static final int LITERAL_BIT_STRING		= 4;

	private static class Literal
	{
		/** type of literal */			int				type;
		/** pointer to parse tree */	Object			pointer;
	};

	/* special codes during VHDL generation */
	/** ordinary block */				private static final int BLOCKNORMAL   =  0;
	/** a MOS transistor */				private static final int BLOCKMOSTRAN  =  1;
	/** a buffer */						private static final int BLOCKBUFFER   =  2;
	/** an and, or, xor */				private static final int BLOCKPOSLOGIC =  3;
	/** an inverter */					private static final int BLOCKINVERTER =  4;
	/** a nand */						private static final int BLOCKNAND     =  5;
	/** a nor */						private static final int BLOCKNOR      =  6;
	/** an xnor */						private static final int BLOCKXNOR     =  7;
	/** a settable D flip-flop */		private static final int BLOCKFLOPDS   =  8;
	/** a resettable D flip-flop */		private static final int BLOCKFLOPDR   =  9;
	/** a settable T flip-flop */		private static final int BLOCKFLOPTS   = 10;
	/** a resettable T flip-flop */		private static final int BLOCKFLOPTR   = 11;
	/** a general flip-flop */			private static final int BLOCKFLOP     = 12;

	private static String	delimiterStr = "&'()*+,-./:;<=>|";
	private static String	doubleDelimiterStr = "=>..**:=/=>=<=<>";

	private static VKeyword [] theKeywords =
	{
		new VKeyword("abs",				KEY_ABS),
		new VKeyword("after",			KEY_AFTER),
		new VKeyword("alias",			KEY_ALIAS),
		new VKeyword("all",				KEY_ALL),
		new VKeyword("and",				KEY_AND),
		new VKeyword("architecture",	KEY_ARCHITECTURE),
		new VKeyword("array",			KEY_ARRAY),
		new VKeyword("assertion",		KEY_ASSERTION),
		new VKeyword("attribute",		KEY_ATTRIBUTE),
		new VKeyword("begin",			KEY_BEGIN),
		new VKeyword("behavioral",		KEY_BEHAVIORAL),
		new VKeyword("body",			KEY_BODY),
		new VKeyword("case",			KEY_CASE),
		new VKeyword("component",		KEY_COMPONENT),
		new VKeyword("connect",			KEY_CONNECT),
		new VKeyword("constant",		KEY_CONSTANT),
		new VKeyword("convert",			KEY_CONVERT),
		new VKeyword("dot",				KEY_DOT),
		new VKeyword("downto",			KEY_DOWNTO),
		new VKeyword("else",			KEY_ELSE),
		new VKeyword("elsif",			KEY_ELSIF),
		new VKeyword("end",				KEY_END),
		new VKeyword("entity",			KEY_ENTITY),
		new VKeyword("exit",			KEY_EXIT),
		new VKeyword("for",				KEY_FOR),
		new VKeyword("function",		KEY_FUNCTION),
		new VKeyword("generate",		KEY_GENERATE),
		new VKeyword("generic",			KEY_GENERIC),
		new VKeyword("if",				KEY_IF),
		new VKeyword("in",				KEY_IN),
		new VKeyword("inout",			KEY_INOUT),
		new VKeyword("is",				KEY_IS),
		new VKeyword("library",			KEY_LIBRARY),
		new VKeyword("linkage",			KEY_LINKAGE),
		new VKeyword("loop",			KEY_LOOP),
		new VKeyword("map",				KEY_MAP),
		new VKeyword("mod",				KEY_MOD),
		new VKeyword("nand",			KEY_NAND),
		new VKeyword("next",			KEY_NEXT),
		new VKeyword("nor",				KEY_NOR),
		new VKeyword("not",				KEY_NOT),
		new VKeyword("null",			KEY_NULL),
		new VKeyword("of",				KEY_OF),
		new VKeyword("open",			KEY_OPEN),
		new VKeyword("or",				KEY_OR),
		new VKeyword("others",			KEY_OTHERS),
		new VKeyword("out",				KEY_OUT),
		new VKeyword("package",			KEY_PACKAGE),
		new VKeyword("port",			KEY_PORT),
		new VKeyword("range",			KEY_RANGE),
		new VKeyword("record",			KEY_RECORD),
		new VKeyword("rem",				KEY_REM),
		new VKeyword("report",			KEY_REPORT),
		new VKeyword("resolve",			KEY_RESOLVE),
		new VKeyword("return",			KEY_RETURN),
		new VKeyword("severity",		KEY_SEVERITY),
		new VKeyword("signal",			KEY_SIGNAL),
		new VKeyword("standard",		KEY_STANDARD),
		new VKeyword("static",			KEY_STATIC),
		new VKeyword("subtype",			KEY_SUBTYPE),
		new VKeyword("then",			KEY_THEN),
		new VKeyword("to",				KEY_TO),
		new VKeyword("type",			KEY_TYPE),
		new VKeyword("units",			KEY_UNITS),
		new VKeyword("use",				KEY_USE),
		new VKeyword("variable",		KEY_VARIABLE),
		new VKeyword("when",			KEY_WHEN),
		new VKeyword("while",			KEY_WHILE),
		new VKeyword("with",			KEY_WITH),
		new VKeyword("xor",				KEY_XOR)
	};

	private Cell vhdlCell;
	private HashSet<String> identTable;
	private TokenList  tListStart;
	private TokenList  tListEnd;
	private int errorCount;
	private boolean hasErrors;
	private UnResList  unResolvedList;


	/**
	 * The constructor compiles the VHDL and produces a netlist.
	 */
	public CompileVHDL(Cell vhdlCell)
	{
		this.vhdlCell = vhdlCell;
		hasErrors = true;
		String [] strings = vhdlCell.getTextViewContents();
		if (strings == null)
		{
			System.out.println("Cell " + vhdlCell.describe(true) + " has no text in it");
			return;
		}

		// initialize
		unResolvedList = null;

		// build and clear identTable
		identTable = new HashSet<String>();

		errorCount = 0;
		doScanner(strings);
		if (doParser(tListStart)) return;
		if (doSemantic()) return;
		hasErrors = false;
	}

	/**
	 * Method to report whether the VHDL compile was successful.
	 * @return true if there were errors.
	 */
	public boolean hasErrors() { return hasErrors; };

	/**
	 * Method to generate a QUISC (silicon compiler) netlist.
     * @param destLib destination library.
	 * @return a List of strings with the netlist.
	 */
	public List<String> getQUISCNetlist(Library destLib)
	{
		// now produce the netlist
		if (hasErrors) return null;
		List<String> netlistStrings = genQuisc(destLib);
		return netlistStrings;
	}

	/**
	 * Method to generate an ALS (simulation) netlist.
     * @param destLib destination library.
	 * @return a List of strings with the netlist.
	 */
	public List<String> getALSNetlist(Library destLib)
	{
		// now produce the netlist
		if (hasErrors) return null;
		Library behaveLib = null;
		List<String> netlistStrings = genALS(destLib, behaveLib);
		return netlistStrings;
	}

	/******************************** THE VHDL SCANNER ********************************/

	/**
	 * Method to do lexical scanning of input VHDL and create token list.
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
				VKeyword key = isKeyword(buf.substring(bufPos, end));
				if (key != null)
				{
					new TokenList(TOKEN_KEYWORD, key, lineNum, space);
				} else
				{
					String ident = buf.substring(bufPos, end);
					identTable.add(ident);
					new TokenList(TOKEN_IDENTIFIER, ident, lineNum, space);
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
				new TokenList(TOKEN_DECIMAL, buf.substring(bufPos, end), lineNum, space);
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
						String newString = buf.substring(bufPos + 1, end);
						newString.replaceAll("\"\"", "\"");
						new TokenList(TOKEN_STRING, newString, lineNum, space);
						if (buf.charAt(end) == '"') end++;
						bufPos = end;
						break;
					case '&':
						new TokenList(TOKEN_AMPERSAND, null, lineNum, space);
						bufPos++;
						break;
					case '\'':
						// character literal
						if (bufPos+2 < buf.length() && buf.charAt(bufPos+2) == '\'')
						{
							new TokenList(TOKEN_CHAR, new Character(buf.charAt(bufPos+1)), lineNum, space);
							bufPos += 3;
						} else
							bufPos++;
						break;
					case '(':
						new TokenList(TOKEN_LEFTBRACKET, null, lineNum, space);
						bufPos++;
						break;
					case ')':
						new TokenList(TOKEN_RIGHTBRACKET, null, lineNum, space);
						bufPos++;
						break;
					case '*':
						// could be STAR or DOUBLESTAR
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '*')
						{
							new TokenList(TOKEN_DOUBLESTAR, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TOKEN_STAR, null, lineNum, space);
							bufPos++;
						}
						break;
					case '+':
						new TokenList(TOKEN_PLUS, null, lineNum, space);
						bufPos++;
						break;
					case ',':
						new TokenList(TOKEN_COMMA, null, lineNum, space);
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
							new TokenList(TOKEN_MINUS, null, lineNum, space);
							bufPos++;
						}
						break;
					case '.':
						// could be PERIOD or DOUBLEDOT
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '.')
						{
							new TokenList(TOKEN_DOUBLEDOT, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TOKEN_PERIOD, null, lineNum, space);
							bufPos++;
						}
						break;
					case '/':
						// could be SLASH or NE
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '=')
						{
							new TokenList(TOKEN_NE, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TOKEN_SLASH, null, lineNum, space);
							bufPos++;
						}
						break;
					case ':':
						// could be COLON or VARASSIGN
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '=')
						{
							new TokenList(TOKEN_VARASSIGN, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TOKEN_COLON, null, lineNum, space);
							bufPos++;
						}
						break;
					case ';':
						new TokenList(TOKEN_SEMICOLON, null, lineNum, space);
						bufPos++;
						break;
					case '<':
						// could be LT or LE or BOX
						if (bufPos+1 < buf.length())
						{
							if (buf.charAt(bufPos+1) == '=')
							{
								new TokenList(TOKEN_LE, null, lineNum, space);
								bufPos += 2;
								break;
							}
							if (buf.charAt(bufPos+1) == '>')
							{
								new TokenList(TOKEN_BOX, null, lineNum, space);
								bufPos += 2;
								break;
							}
						}
						new TokenList(TOKEN_LT, null, lineNum, space);
						bufPos++;
						break;
					case '=':
						// could be EQUAL or double delimiter ARROW
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '>')
						{
							new TokenList(TOKEN_ARROW, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TOKEN_EQ, null, lineNum, space);
							bufPos++;
						}
						break;
					case '>':
						// could be GT or GE
						if (bufPos+1 < buf.length() && buf.charAt(bufPos+1) == '=')
						{
							new TokenList(TOKEN_GE, null, lineNum, space);
							bufPos += 2;
						} else
						{
							new TokenList(TOKEN_GT, null, lineNum, space);
							bufPos++;
						}
						break;
					case '|':
						new TokenList(TOKEN_VERTICALBAR, null, lineNum, space);
						bufPos++;
						break;
					default:
						new TokenList(TOKEN_UNKNOWN, null, lineNum, space);
						bufPos++;
						break;
				}
			}
		}
	}

	/**
	 * Method to get address in the keyword table.
	 * @param tString string to lookup.
	 * @return entry in keywords table if keyword, else null.
	 */
	public static VKeyword isKeyword(String tString)
	{
		int base = 0;
		int num = theKeywords.length;
		int aIndex = num >> 1;
		while (num != 0)
		{
			int check = tString.compareTo(theKeywords[base + aIndex].name);
			if (check == 0) return theKeywords[base + aIndex];
			if (check < 0)
			{
				num = aIndex;
				aIndex = num >> 1;
			} else
			{
				base += aIndex + 1;
				num -= aIndex + 1;
				aIndex = num >> 1;
			}
		}
		return null;
	}

	/******************************** THE VHDL PARSER ********************************/

	private boolean			hasError;
	private TokenList		nextToken;
	private PTree			pTree;

	private class ParseException extends Exception {}

	/**
	 * Method to parse the passed token list using the parse tables.
	 * Reports on any syntax errors and create the required syntax trees.
	 * @param tlist list of tokens.
	 */
	private boolean doParser(TokenList tList)
	{
		hasError = false;
		pTree = null;
		PTree endunit = null;
		nextToken = tList;
		try
		{
			while (nextToken != null)
			{
				if (nextToken.token == TOKEN_KEYWORD)
				{
					int type = NOUNIT;
					VKeyword vk = (VKeyword)nextToken.pointer;
					Object pointer = null;
					switch (vk.num)
					{
						case KEY_LIBRARY:
							parseToSemicolon();
							break;
						case KEY_ENTITY:
							type = UNIT_INTERFACE;
							pointer = parseInterface();
							break;
						case KEY_ARCHITECTURE:
							type = UNIT_BODY;
							pointer = parseBody();
							break;
						case KEY_PACKAGE:
							type = UNIT_PACKAGE;
							pointer = parsePackage();
							break;
						case KEY_USE:
							type = UNIT_USE;
							pointer = parseUse();
							break;
						default:
							reportErrorMsg(nextToken, "No entry keyword - entity, architectural, behavioral");
							nextToken = nextToken.next;
							break;
					}
					if (type != NOUNIT)
					{
						PTree newUnit = new PTree();
						newUnit.type = type;
						newUnit.pointer = pointer;
						newUnit.next = null;
						if (endunit == null)
						{
							pTree = endunit = newUnit;
						} else
						{
							endunit.next = newUnit;
							endunit = newUnit;
						}
					}
				} else
				{
					reportErrorMsg(nextToken, "No entry keyword - entity, architectural, behavioral");
					nextToken = nextToken.next;
				}
			}
		} catch (ParseException e)
		{
		}
		return hasError;
	}

	/**
	 * Method to parse an interface description of the form:
	 *    ENTITY identifier IS PORT (formal_port_list);
	 *    END [identifier] ;
	 */
	private VInterface parseInterface()
		throws ParseException
	{
		getNextToken();

		// check for entity IDENTIFIER
		TokenList name = null;
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
		} else
		{
			name = nextToken;
		}

		// check for keyword IS
		getNextToken();
		if (!isKeySame(nextToken, KEY_IS))
		{
			reportErrorMsg(nextToken, "Expecting keyword IS");
		}

		// check for keyword PORT
		getNextToken();
		if (!isKeySame(nextToken, KEY_PORT))
		{
			reportErrorMsg(nextToken, "Expecting keyword PORT");
		}

		// check for opening bracket of FORMAL_PORT_LIST
		getNextToken();
		if (nextToken.token != TOKEN_LEFTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a left bracket");
		}

		// gather FORMAL_PORT_LIST
		getNextToken();
		FPortList ports = parseFormalPortList();
		if (ports == null)
		{
			reportErrorMsg(nextToken, "Interface must have ports");
		}

		// check for closing bracket of FORMAL_PORT_LIST
		if (nextToken.token != TOKEN_RIGHTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a right bracket");
		}

		getNextToken();
		// check for SEMICOLON
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		else getNextToken();

		// check for keyword END
		if (!isKeySame(nextToken, KEY_END))
		{
			reportErrorMsg(nextToken, "Expecting keyword END");
		}

		// check for optional entity IDENTIFIER
		getNextToken();
		if (nextToken.token == TOKEN_IDENTIFIER)
		{
			if (!nextToken.pointer.equals(name.pointer))
			{
				reportErrorMsg(nextToken, "Unmatched entity identifier names");
			}
			getNextToken();
		}

		// check for closing SEMICOLON
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		nextToken = nextToken.next;

		// allocate an entity parse tree
		VInterface interfacef = new VInterface();
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
	 * @return the created body structure.
	 */
	private Body parseBody()
		throws ParseException
	{
		getNextToken();

		// next is bodyName (identifier)
		TokenList bodyName = null;
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
		} else
		{
			bodyName = nextToken;
		}
		getNextToken();

		// check for keyword OF
		if (!isKeySame(nextToken, KEY_OF))
		{
			reportErrorMsg(nextToken, "Expecting keyword OF");
		}
		getNextToken();

		// next is design entity reference for this body (simple_name)
		SimpleName entityName = parseSimpleName();

		// check for keyword IS
		if (!isKeySame(nextToken, KEY_IS))
		{
			reportErrorMsg(nextToken, "Expecting keyword IS");
		}
		getNextToken();

		// body declaration part
		BodyDeclare bodyDeclare = parseBodyDeclare();

		// should be at keyword BEGIN
		if (!isKeySame(nextToken, KEY_BEGIN))
		{
			reportErrorMsg(nextToken, "Expecting keyword BEGIN");
		}
		getNextToken();

		// statements of body
		Statements statements = parseSetOfStatements();

		// should be at keyword END
		if (!isKeySame(nextToken, KEY_END))
		{
			reportErrorMsg(nextToken, "Expecting keyword END");
		}
		getNextToken();

		// optional body name
		if (nextToken.token == TOKEN_IDENTIFIER)
		{
			if (!nextToken.pointer.equals(bodyName.pointer))
			{
				reportErrorMsg(nextToken, "Body name mismatch");
			}
			getNextToken();
		}

		// should be at final semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		nextToken = nextToken.next;

		// create body parse tree
		Body body = new Body();
		body.name = bodyName;
		body.entity = entityName;
		body.bodyDeclare = bodyDeclare;
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
	private Package parsePackage()
		throws ParseException
	{
		Package vPackage = null;

		// should be at keyword package
		if (!isKeySame(nextToken, KEY_PACKAGE))
		{
			reportErrorMsg(nextToken, "Expecting keyword PACKAGE");
			getNextToken();
			return vPackage;
		}
		getNextToken();

		// should be package identifier
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
			getNextToken();
			return vPackage;
		}
		TokenList identifier = nextToken;
		getNextToken();

		// should be at keyword IS
		if (!isKeySame(nextToken, KEY_IS))
		{
			reportErrorMsg(nextToken, "Expecting keyword IS");
			getNextToken();
			return vPackage;
		}
		getNextToken();

		// package declarative part
		PackagedPart declarePart = parsePackageDeclarePart();

		// should be at keyword END
		if (!isKeySame(nextToken, KEY_END))
		{
			reportErrorMsg(nextToken, "Expecting keyword END");
			getNextToken();
			return vPackage;
		}
		getNextToken();

		// check for optional end identifier
		if (nextToken.token == TOKEN_IDENTIFIER)
		{
			if (!nextToken.pointer.equals(identifier.pointer))
			{
				reportErrorMsg(nextToken, "Name mismatch");
				getNextToken();
				return vPackage;
			}
			getNextToken();
		}

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
			getNextToken();
			return vPackage;
		}
		getNextToken();

		// create package structure
		vPackage = new Package();
		vPackage.name = identifier;
		vPackage.declare = declarePart;

		return vPackage;
	}

	/**
	 * Method to parse a use clause.
	 * It has the form:
	 *    use_clause ::= USE unit {,unit} ;
	 *    unit ::= package_name.ALL
	 * @return the use clause structure.
	 */
	private Use parseUse()
		throws ParseException
	{
		Use use = null;

		// should be at keyword USE
		if (!isKeySame(nextToken, KEY_USE))
		{
			reportErrorMsg(nextToken, "Expecting keyword USE");
			getNextToken();
			return use;
		}
		getNextToken();

		// must be at least one unit
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Bad unit name for use clause");
			getNextToken();
			return use;
		}
		use = new Use();
		use.unit = nextToken;
		use.next = null;
		Use endUse = use;
		getNextToken();

		// IEEE version uses form unit.ALL only
		for(;;)
		{
			if (nextToken.token != TOKEN_PERIOD)
			{
				reportErrorMsg(nextToken, "Expecting period");
				break;
			}
			getNextToken();

			if (isKeySame(nextToken, KEY_ALL))
			{
				getNextToken();
				break;
			}
			if (nextToken.token != TOKEN_IDENTIFIER)
			{
				reportErrorMsg(nextToken, "Bad unit name for use clause");
				break;
			}
			getNextToken();
		}

		while (nextToken.token == TOKEN_COMMA)
		{
			getNextToken();
			if (nextToken.token != TOKEN_IDENTIFIER)
			{
				reportErrorMsg(nextToken, "Bad unit name for use clause");
				getNextToken();
				return use;
			}
			Use newUse = new Use();
			newUse.unit = nextToken;
			newUse.next = null;
			endUse.next = newUse;
			endUse = newUse;
			getNextToken();

			// IEEE version uses form unit.ALL only
			if (nextToken.token == TOKEN_PERIOD)
				getNextToken();
			else reportErrorMsg(nextToken, "Expecting period");

			if (isKeySame(nextToken, KEY_ALL))
				getNextToken();
			else reportErrorMsg(nextToken, "Expecting keyword ALL");
		}

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		getNextToken();

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
	private PackagedPart parsePackageDeclarePart()
		throws ParseException
	{
		PackagedPart dPart = null;

		// should be at least one
		if (isKeySame(nextToken, KEY_END))
		{
			reportErrorMsg(nextToken, "No Package declarative part");
			return dPart;
		}
		BasicDeclare dItem = parseBasicDeclare();
		dPart = new PackagedPart();
		dPart.item = dItem;
		dPart.next = null;
		PackagedPart endPart = dPart;

		while (!isKeySame(nextToken, KEY_END))
		{
			dItem = parseBasicDeclare();
			PackagedPart newpart = new PackagedPart();
			newpart.item = dItem;
			newpart.next = null;
			endPart.next = newpart;
			endPart = newpart;
		}

		return dPart;
	}

	/**
	 * Method to parse the body statements and return pointer to the parse tree.
	 * The form of body statements are:
	 *    set_of_statements :== architectural_statement {architectural_statement}
	 *    architectural_statement :== generate_statement | signal_assignment_statement | architectural_if_statement | architectural_case_statement | component_instantiation_statement | null_statement
	 * @return the statements parse tree.
	 */
	private Statements parseSetOfStatements()
		throws ParseException
	{
		Statements statements = null;
		Statements endState = null;
		while (!isKeySame(nextToken, KEY_END))
		{
			int type = NOARCHSTATE;
			Object pointer = null;

			// check for case statement
			if (isKeySame(nextToken, KEY_CASE))
			{
				// EMPTY
			}

			// check for null statement
			else if (isKeySame(nextToken, KEY_NULL))
			{
				type = ARCHSTATE_NULL;
				pointer = null;
				getNextToken();

				// should be a semicolon
				if (nextToken.token != TOKEN_SEMICOLON)
				{
					reportErrorMsg(nextToken, "Expecting a semicolon");
				}
				getNextToken();
			}

			// check for label
			else if (nextToken.token == TOKEN_IDENTIFIER && nextToken.next != null &&
					nextToken.next.token == TOKEN_COLON)
			{
				TokenList label = nextToken;
				getNextToken();
				getNextToken();

				// check for generate statement
				if (isKeySame(nextToken, KEY_IF))
				{
					type = ARCHSTATE_GENERATE;
					pointer = parseGenerate(label, GENSCHEME_IF);
				}
				else if (isKeySame(nextToken, KEY_FOR))
				{
					type = ARCHSTATE_GENERATE;
					pointer = parseGenerate(label, GENSCHEME_FOR);
				}
				// should be component_instantiation_declaration
				else
				{
					nextToken = label;
					type = ARCHSTATE_INSTANCE;
					pointer = parseInstance();
				}
			}

			// add statement if found
			if (type != NOARCHSTATE)
			{
				Statements newState = new Statements();
				newState.type = type;
				newState.pointer = pointer;
				newState.next = null;
				if (endState == null)
				{
					statements = endState = newState;
				} else
				{
					endState.next = newState;
					endState = newState;
				}
			} else
			{
				reportErrorMsg(nextToken, "Invalid ARCHITECTURAL statement");
				nextToken = nextToken.next;
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
	private VInstance parseInstance()
		throws ParseException
	{
		VInstance inst = null;

		// check for identifier
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
			getNextToken();
			return inst;
		}
		TokenList name = nextToken;
		getNextToken();

		// if colon, previous token was the label
		if (nextToken.token == TOKEN_COLON)
		{
			getNextToken();
		} else
		{
			nextToken = name;
			name = null;
		}

		// should be at component reference
		SimpleName entity = parseSimpleName();

		// Require PORT MAP
		if (isKeySame(nextToken, KEY_PORT))
			getNextToken();
		else reportErrorMsg(nextToken, "Expecting keyword PORT");

		if (isKeySame(nextToken, KEY_MAP))
			getNextToken();
		else reportErrorMsg(nextToken, "Expecting keyword MAP");

		// should be at left bracket
		if (nextToken.token != TOKEN_LEFTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a left bracket");
		}
		getNextToken();
		APortList ports = parseActualPortList();

		// should be at right bracket
		if (nextToken.token != TOKEN_RIGHTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a right bracket");
		}
		getNextToken();

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		getNextToken();

		inst = new VInstance();
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
	private APortList parseActualPortList()
		throws ParseException
	{
		APortList lastPort = null;

		// should be at least one port association
		APortList apList = new APortList();
		apList.type = APORTLIST_NAME;
		if (nextToken.token != TOKEN_COMMA && nextToken.token != TOKEN_RIGHTBRACKET)
		{
			if (isKeySame(nextToken, KEY_OPEN))
			{
				apList.pointer = null;
				getNextToken();
			} else
			{
				apList.pointer = parseName();
				if (nextToken.token == TOKEN_ARROW)
				{
					getNextToken();
					apList.pointer = parseName();
				}
			}

		}
		else reportErrorMsg(nextToken, "No identifier in port list");

		apList.next = null;
		lastPort = apList;
		while (nextToken.token == TOKEN_COMMA)
		{
			getNextToken();
			APortList newPort = new APortList();
			newPort.type = APORTLIST_NAME;
			if (nextToken.token != TOKEN_COMMA && nextToken.token != TOKEN_RIGHTBRACKET)
			{
				if (isKeySame(nextToken, KEY_OPEN))
				{
					newPort.pointer = null;
					getNextToken();
				} else
				{
					newPort.pointer = parseName();
					if (nextToken.token == TOKEN_ARROW)
					{
						getNextToken();
						newPort.pointer = parseName();
					}
				}
			}
			else reportErrorMsg(nextToken, "No identifier in port list");

			newPort.next = null;
			lastPort.next = newPort;
			lastPort = newPort;
		}
		return apList;
	}

	/**
	 * Method to parse a generate statement.
	 * It has the form:
	 *    generate_statement ::= label: generate_scheme GENERATE set_of_statements END GENERATE [label];
	 *    generate_scheme ::= FOR generate_parameter_specification | IF condition
	 *    generate_parameter_specification ::= identifier IN discrete_range
	 * @param label pointer to optional label.
	 * @param gScheme generate scheme (FOR or IF).
	 * @return generate statement structure.
	 */
	private Generate parseGenerate(TokenList label, int gScheme)
		throws ParseException
	{
		Generate gen = null;

		if (gScheme == GENSCHEME_FOR)
		{
			// should be past label and at keyword FOR
			if (!isKeySame(nextToken, KEY_FOR))
			{
				reportErrorMsg(nextToken, "Expecting keyword FOR");
			}
		} else
		{
			// should be past label and at keyword IF
			if (!isKeySame(nextToken, KEY_IF))
			{
				reportErrorMsg(nextToken, "Expecting keyword IF");
			}
		}
		GenScheme scheme = new GenScheme();
		if (gScheme == GENSCHEME_FOR)
		{
			scheme.scheme = GENSCHEME_FOR;
		} else
		{
			scheme.scheme = GENSCHEME_IF;
		}
		scheme.identifier = null;
		scheme.range = null;
		scheme.condition = null;		// for IF scheme only
		getNextToken();

		if (gScheme == GENSCHEME_FOR)
		{
			// should be generate parameter specification
			if (nextToken.token != TOKEN_IDENTIFIER)
			{
				reportErrorMsg(nextToken, "Expecting an identifier");
			} else
			{
				scheme.identifier = nextToken;
			}
			getNextToken();

			// should be keyword IN
			if (!isKeySame(nextToken, KEY_IN))
			{
				reportErrorMsg(nextToken, "Expecting keyword IN");
			}
			getNextToken();

			// should be discrete range
			scheme.range = parseDiscreteRange();
		} else
		{
			scheme.condition = parseExpression();
		}

		// should be keyword GENERATE
		if (!isKeySame(nextToken, KEY_GENERATE))
		{
			reportErrorMsg(nextToken, "Expecting keyword GENERATE");
		}
		getNextToken();

		// set of statements
		Statements states = parseSetOfStatements();

		// should be at keyword END
		if (!isKeySame(nextToken, KEY_END))
		{
			reportErrorMsg(nextToken, "Expecting keyword END");
		}
		getNextToken();

		// should be at keyword GENERATE
		if (!isKeySame(nextToken, KEY_GENERATE))
		{
			reportErrorMsg(nextToken, "Expecting keyword GENERATE");
		}
		getNextToken();

		// check if label should be present
		if (label != null)
		{
			/* For correct IEEE syntax, label is always true, but trailing
			 * label is optional.
			 */
			if (nextToken.token == TOKEN_IDENTIFIER)
			{
				if (!label.pointer.equals(nextToken.pointer))
					reportErrorMsg(nextToken, "Label mismatch");
				getNextToken();
			}
		}

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		getNextToken();

		// create generate statement structure
		gen = new Generate();
		gen.label = label;
		gen.genScheme = scheme;
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
	private BodyDeclare parseBodyDeclare()
		throws ParseException
	{
		BodyDeclare body =null;
		BodyDeclare endBody = null;
		Object pointer = null;
		int type = 0;
		while (!isKeySame(nextToken, KEY_BEGIN))
		{
			// check for component declaration
			if (isKeySame(nextToken, KEY_COMPONENT))
			{
				type = BODYDECLARE_COMPONENT;
				pointer = parseComponent();
			}
			// check for resolution declaration
			else if (isKeySame(nextToken, KEY_RESOLVE))
			{
				type = BODYDECLARE_RESOLUTION;
				pointer = null;
				getNextToken();
			}
			// check for local function declaration
			else if (isKeySame(nextToken, KEY_FUNCTION))
			{
				type = BODYDECLARE_LOCAL;
				pointer = null;
				getNextToken();
			}
			// should be basic declaration
			else
			{
				type = BODYDECLARE_BASIC;
				pointer = parseBasicDeclare();
			}
			BodyDeclare newBody = new BodyDeclare();
			newBody.type = type;
			newBody.pointer = pointer;
			newBody.next = null;
			if (endBody == null)
			{
				body = endBody = newBody;
			} else
			{
				endBody.next = newBody;
				endBody = newBody;
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
	private BasicDeclare parseBasicDeclare()
		throws ParseException
	{
		BasicDeclare basic = null;
		int type = NOBASICDECLARE;
		Object pointer = null;
		if (isKeySame(nextToken, KEY_TYPE))
		{
			type = BASICDECLARE_TYPE;
			pointer = parseType();
		} else
		{
			type = BASICDECLARE_OBJECT;
			pointer = parseObjectDeclare();
		}
		if (type != NOBASICDECLARE)
		{
			basic = new BasicDeclare();
			basic.type = type;
			basic.pointer = pointer;
		} else getNextToken();	// Bug fix , D.J.Yurach, June, 1988
		return basic;
	}

	/**
	 * Method to parse a type declaration.
	 * It has the form: type_declaration ::= TYPE identifier IS type_definition ;
	 * @return the type declaration structure.
	 */
	private Type parseType()
		throws ParseException
	{
		Type type = null;

		// should be at keyword TYPE
		if (!isKeySame(nextToken, KEY_TYPE))
		{
			reportErrorMsg(nextToken, "Expecting keyword TYPE");
			getNextToken();
			return type;
		}
		getNextToken();

		// should be at type identifier
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
			getNextToken();
			return type;
		}
		TokenList ident = nextToken;
		getNextToken();

		// should be keyword IS
		if (!isKeySame(nextToken, KEY_IS))
		{
			reportErrorMsg(nextToken, "Expecting keyword IS");
			getNextToken();
			return type;
		}
		getNextToken();

		// parse type definition
		Object pointer = null;
		int typeDefine = 0;
		if (isKeySame(nextToken, KEY_ARRAY))
		{
			typeDefine = TYPE_COMPOSITE;
			pointer = parseCompositeType();
		} else if (isKeySame(nextToken, KEY_RECORD))
		{
			typeDefine = TYPE_COMPOSITE;
			pointer = parseCompositeType();
		} else if (isKeySame(nextToken, KEY_RANGE))
		{
			typeDefine = TYPE_SCALAR;
			pointer = null;
		} else if (nextToken.token == TOKEN_LEFTBRACKET)
		{
			typeDefine = TYPE_SCALAR;
			pointer = null;
		} else
		{
			reportErrorMsg(nextToken, "Invalid type definition");
			getNextToken();
			return type;
		}

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
			getNextToken();
			return type;
		}
		getNextToken();

		type = new Type();
		type.identifier = ident;
		type.type = typeDefine;
		type.pointer = pointer;

		return type;
	}

	/**
	 * Method to parse a composite type definition.
	 * It has the form:
	 *    composite_type_definition ::= array_type_definition | record_type_definition
	 */
	private Composite parseCompositeType()
		throws ParseException
	{
		Composite compo = null;

		// should be ARRAY or RECORD keyword
		Object pointer = null;
		int type = 0;
		if (isKeySame(nextToken, KEY_ARRAY))
		{
			type = COMPOSITE_ARRAY;
			pointer = parseArrayType();
		} else if (isKeySame(nextToken, KEY_RECORD))
		{
			type = COMPOSITE_RECORD;
			pointer = null;
		} else
		{
			reportErrorMsg(nextToken, "Invalid composite type");
			getNextToken();
			return compo;
		}

		compo = new Composite();
		compo.type = type;
		compo.pointer = pointer;

		return compo;
	}

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
	private Array parseArrayType()
		throws ParseException
	{
		Array array = null;

		// should be keyword ARRAY
		if (!isKeySame(nextToken, KEY_ARRAY))
		{
			reportErrorMsg(nextToken, "Expecting keyword ARRAY");
			getNextToken();
			return array;
		}
		getNextToken();

		// index_constraint
		// should be left bracket
		if (nextToken.token != TOKEN_LEFTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a left bracket");
			getNextToken();
			return array;
		}
		getNextToken();

		// should at least one discrete range
		IndexConstraint iConstraint = new IndexConstraint();
		iConstraint.discrete = parseDiscreteRange();
		iConstraint.next = null;
		IndexConstraint endconstraint = iConstraint;

		// continue while comma
		while (nextToken.token == TOKEN_COMMA)
		{
			getNextToken();
			IndexConstraint newConstraint = new IndexConstraint();
			newConstraint.discrete = parseDiscreteRange();
			newConstraint.next = null;
			endconstraint.next = newConstraint;
			endconstraint = newConstraint;
		}

		// should be at right bracket
		if (nextToken.token != TOKEN_RIGHTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a right bracket");
			getNextToken();
			return array;
		}
		getNextToken();

		// should be at keyword OF
		if (!isKeySame(nextToken, KEY_OF))
		{
			reportErrorMsg(nextToken, "Expecting keyword OF");
			getNextToken();
			return array;
		}
		getNextToken();

		// subtype_indication
		SubTypeInd subType = parseSubtypeIndication();

		// create array type definition
		array = new Array();
		array.type = ARRAY_CONSTRAINED;
		Constrained constr = new Constrained();
		array.pointer = constr;
		constr.constraint = iConstraint;
		constr.subType = subType;

		return array;
	}

	/**
	 * Method to parse a discrete range.
	 * The range has the form: discrete_range ::= subtype_indication | range
	 * @return the discrete range structure.
	 */
	private DiscreteRange parseDiscreteRange()
		throws ParseException
	{
		DiscreteRange dRange = new DiscreteRange();

		// currently only support range option
		dRange.type = DISCRETERANGE_RANGE;
		dRange.pointer = parseRange();
		return dRange;
	}

	/**
	 * Method to parse a range.
	 * The range has the form:
	 *    range :== simple_expression direction simple_expression
	 *    direction ::=  TO  |  DOWNTO
	 * @return the range structure.
	 */
	private Range parseRange()
		throws ParseException
	{
		Range range = new Range();

		// currently support only simple expression range option
		range.type = RANGE_SIMPLE_EXPR;
		range.pointer = parseRangeSimple();
		return range;
	}

	/**
	 * Method to parse a simple expression range.
	 * The range has the form: simple_expression .. simple_expression
	 * @return the simple expression range.
	 */
	private RangeSimple parseRangeSimple()
		throws ParseException
	{
		RangeSimple sRange = new RangeSimple();
		sRange.start = parseSimpleExpression();

		// Need keyword TO or DOWNTO
		if (isKeySame(nextToken, KEY_TO) || isKeySame(nextToken, KEY_DOWNTO))
			getNextToken();
		else
		{
			reportErrorMsg(nextToken, "Expecting keyword TO or DOWNTO");
			getNextToken(); // absorb the token anyway (probably "..")
		}

		sRange.end = parseSimpleExpression();
		return sRange;
	}

	/**
	 * Method to parse an object declaration and return the pointer to its parse tree.
	 * An object declaration has the form:
	 *    object_declaration :== constant_declaration | signal_declaration | variable_declaration | alias_declaration
	 * @return the object declaration parse tree.
	 */
	private ObjectDeclare parseObjectDeclare()
		throws ParseException
	{
		ObjectDeclare object = null;
		int type = NOOBJECTDECLARE;
		Object pointer = null;
		if (isKeySame(nextToken, KEY_CONSTANT))
		{
			type = OBJECTDECLARE_CONSTANT;
			pointer = parseConstantDeclare();
		} else if (isKeySame(nextToken, KEY_SIGNAL))
		{
			type = OBJECTDECLARE_SIGNAL;
			pointer = parseSignalDeclare();
		} else if (isKeySame(nextToken, KEY_VARIABLE))
		{
			// EMPTY
		} else if (isKeySame(nextToken, KEY_ALIAS))
		{
			// EMPTY
		} else
		{
			reportErrorMsg(nextToken, "Invalid object declaration");
		}
		if (type != NOOBJECTDECLARE)
		{
			object = new ObjectDeclare();
			object.type = type;
			object.pointer = pointer;
		} else
		{
			getNextToken();
		}
		return object;
	}

	/**
	 * Method to parse a constant declaration and return the pointer to the parse tree.
	 * The form of a constant declaration is:
	 *    constant_declaration :== CONSTANT identifier : subtype_indication := expression ;
	 * @return the constant declaration parse tree.
	 */
	private ConstantDeclare parseConstantDeclare()
		throws ParseException
	{
		ConstantDeclare constant = null;
		getNextToken();

		// parse identifier
		// Note that the standard allows identifier_list here, but we don't support it!
		TokenList ident = null;
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
		} else
		{
			ident = nextToken;
		}
		getNextToken();

		// should be at colon
		if (nextToken.token != TOKEN_COLON)
		{
			reportErrorMsg(nextToken, "Expecting a colon");
		}
		getNextToken();

		// parse subtype indication
		SubTypeInd ind = parseSubtypeIndication();

		// should be at assignment symbol
		if (nextToken.token != TOKEN_VARASSIGN)
		{
			reportErrorMsg(nextToken, "Expecting variable assignment symbol");
		}
		getNextToken();

		// should be at expression
		Expression expr = parseExpression();

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		getNextToken();

		constant = new ConstantDeclare();
		constant.identifier = ident;
		constant.subType = ind;
		constant.expression = expr;

		return constant;
	}

	/**
	 * Method to parse a signal declaration and return the pointer to the parse tree.
	 * The form of a signal declaration is:
	 *    signal_declaration :== SIGNAL identifier_list : subtype_indication;
	 * @return the signal declaration parse tree.
	 */
	private SignalDeclare parseSignalDeclare()
		throws ParseException
	{
		getNextToken();

		// parse identifier list
		IdentList signalList = parseIdentList();

		// should be at colon
		if (nextToken.token != TOKEN_COLON)
		{
			reportErrorMsg(nextToken, "Expecting a colon");
		}
		getNextToken();

		// parse subtype indication
		SubTypeInd ind = parseSubtypeIndication();

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		getNextToken();

		SignalDeclare signal = new SignalDeclare();
		signal.names = signalList;
		signal.subType = ind;
		return signal;
	}

	/**
	 * Method to parse a subtype indicatio.
	 * It has the form: subtype_indication :== type_mark [constraint]
	 * @return subtype indication parse tree.
	 */
	private SubTypeInd parseSubtypeIndication()
		throws ParseException
	{
		VName type = parseName();
		SubTypeInd ind = new SubTypeInd();
		ind.type = type;
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
	private VComponent parseComponent()
		throws ParseException
	{
		VComponent compo =  null;
		getNextToken();

		// should be component identifier
		TokenList entity = null;
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
		} else
		{
			entity = nextToken;
		}
		getNextToken();

		// Need keyword PORT
		if (!isKeySame(nextToken,KEY_PORT))
			reportErrorMsg(nextToken, "Expecting keyword PORT");
		else getNextToken();

		// should be left bracket, start of port list
		if (nextToken.token != TOKEN_LEFTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a left bracket");
		}
		getNextToken();

		// go through port list
		FPortList ports = parseFormalPortList();

		// should be pointing to RIGHTBRACKET
		if (nextToken.token != TOKEN_RIGHTBRACKET)
		{
			reportErrorMsg(nextToken, "Expecting a right bracket");
		}
		getNextToken();

		// should be at semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		getNextToken();

		// Need "END COMPONENT"
		if (!isKeySame(nextToken, KEY_END))
			reportErrorMsg(nextToken, "Expecting keyword END");
		getNextToken();

		if (!isKeySame(nextToken, KEY_COMPONENT))
			reportErrorMsg(nextToken, "Expecting keyword COMPONENT");
		getNextToken();

		// should be at terminating semicolon
		if (nextToken.token != TOKEN_SEMICOLON)
		{
			reportErrorMsg(nextToken, "Expecting a semicolon");
		}
		getNextToken();
		compo = new VComponent();
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
	private FPortList parseFormalPortList()
		throws ParseException
	{
		// must be at least one port declaration
		IdentList iList = parseIdentList();
		if (iList == null) return null;

		// should be at colon
		if (nextToken.token != TOKEN_COLON)
		{
			reportErrorMsg(nextToken, "Expecting a colon");
			return null;
		}
		getNextToken();
		// Get port mode
		int mode = parsePortMode();
		// should be at type_mark
		VName type = parseName();

		// create port declaration
		FPortList ports = new FPortList();
		ports.names = iList;
		ports.mode = mode;
		ports.type = type;
		ports.next = null;
		FPortList endPort = ports;

		while (nextToken.token == TOKEN_SEMICOLON)
		{
			getNextToken();
			iList = parseIdentList();
			if (iList == null) return null;

			// should be at colon
			if (nextToken.token != TOKEN_COLON)
			{
				reportErrorMsg(nextToken, "Expecting a colon");
				return null;
			}
			getNextToken();

			// Get port mode
			mode = parsePortMode();

			// should be at type_mark
			type = parseName();
			FPortList newPort = new FPortList();
			newPort.names = iList;
			newPort.mode = mode;
			newPort.type = type;
			newPort.next = null;
			endPort.next = newPort;
			endPort = newPort;
		}

		return ports;
	}

	/**
	 * Method to parse a port mode description.
	 * The description has the form:
	 *    port_mode :== [in] | [ dot ] out | inout | linkage
	 * @return type of mode (default to in).
	 */
	private int parsePortMode()
		throws ParseException
	{
		int mode = MODE_IN;
		if (nextToken.token == TOKEN_KEYWORD)
		{
			switch (((VKeyword)(nextToken.pointer)).num)
			{
				case KEY_IN:
					getNextToken();
					break;
				case KEY_OUT:
					mode = MODE_OUT;
					getNextToken();
					break;
				case KEY_INOUT:
					mode = MODE_INOUT;
					getNextToken();
					break;
				case KEY_LINKAGE:
					mode = MODE_LINKAGE;
					getNextToken();
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
	private VName parseName()
		throws ParseException
	{
		int type = NONAME;
		Object pointer = parseSingleName();

		switch (nextToken.token)
		{
			case TOKEN_AMPERSAND:
				type = NAME_CONCATENATE;
				ConcatenatedName concat = new ConcatenatedName();
				concat.name = (SingleName)pointer;
				concat.next = null;
				pointer = concat;
				while (nextToken.token == TOKEN_AMPERSAND)
				{
					getNextToken();
					SingleName pointer2 = parseSingleName();
					ConcatenatedName concat2 = new ConcatenatedName();
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

		VName name = null;
		if (type != NONAME)
		{
			name = new VName();
			name.type = type;
			name.pointer = pointer;
		} else
		{
			getNextToken();
		}
		return name;
	}

	/**
	 * Method to parse a single name.
	 * Single names are of the form:
	 *    single_name :== simple_name | selected_name | indexed_name | slice_name
	 * @return the single name structure.
	 */
	private SingleName parseSingleName()
		throws ParseException
	{
		int type = NOSINGLENAME;
		SingleName sName = null;
		Object pointer = parseSimpleName();

		if (nextToken.last.space)
		{
			type = SINGLENAME_SIMPLE;
		} else
		{
			switch (nextToken.token)
			{
				case TOKEN_PERIOD:
					break;
				case TOKEN_LEFTBRACKET:
					// could be a indexed_name or a slice_name
					// but support only indexed names
					getNextToken();
					type = SINGLENAME_INDEXED;
					VName nPtr = new VName();
					nPtr.type = NAME_SINGLE;
					SingleName sName2 = new SingleName();
					nPtr.pointer = sName2;
					sName2.type = SINGLENAME_SIMPLE;
					sName2.pointer = pointer;
					pointer = parseIndexedName(PREFIX_NAME, nPtr);
					// should be at right bracket
					if (nextToken.token != TOKEN_RIGHTBRACKET)
					{
						reportErrorMsg(nextToken, "Expecting a right bracket");
					}
					getNextToken();
					break;
				default:
					type = SINGLENAME_SIMPLE;
					break;
			}
		}

		if (type != NOSINGLENAME)
		{
			sName = new SingleName();
			sName.type = type;
			sName.pointer = pointer;
		} else
		{
			getNextToken();
		}
		return sName;
	}

	/**
	 * Method to parse a simple name.
	 * The name has the form:
	 *    simple_name ::= identifier
	 * @return pointer to simple name structure.
	 */
	private SimpleName parseSimpleName()
		throws ParseException
	{
		SimpleName sName = null;
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
			getNextToken();
			return sName;
		}
		sName = new SimpleName();
		sName.identifier = nextToken;
		getNextToken();
		return sName;
	}

	/**
	 * Method to parse an indexed name given its prefix and now at the index.
	 * The form of an indexed name is: indexed_name ::= prefix(expression{, expression})
	 * @param preType type of prefix (VName or FUNCTION CALL).
	 * @param prePtr pointer to prefix structure.
	 * @return pointer to indexed name.
	 */
	private IndexedName parseIndexedName(int preType, VName prePtr)
		throws ParseException
	{
		Prefix prefix = new Prefix();
		prefix.type = preType;
		prefix.pointer = prePtr;
		IndexedName ind = new IndexedName();
		ind.prefix = prefix;
		ind.exprList = new ExprList();
		ind.exprList.expression = parseExpression();
		ind.exprList.next = null;
		ExprList eList = ind.exprList;

		// continue while at a comma
		while (nextToken.token == TOKEN_COMMA)
		{
			getNextToken();
			ExprList newEList = new ExprList();
			newEList.expression = parseExpression();
			newEList.next = null;
			eList.next = newEList;
			eList = newEList;
		}

		return ind;
	}

	/**
	 * Method to parse an expression of the form:
	 *    expression ::= relation {AND relation} | relation {OR relation} | relation {NAND relation} | relation {NOR relation} | relation {XOR relation}
	 * @return the expression structure.
	 */
	private Expression parseExpression()
		throws ParseException
	{
		Expression exp = new Expression();
		exp.relation = parseRelation();
		exp.next = null;

		// check for more terms
		int key = 0;
		int logOp = NOLOGOP;
		if (nextToken.token == TOKEN_KEYWORD)
		{
			key = ((VKeyword)(nextToken.pointer)).num;
			switch (key)
			{
				case KEY_AND:
					logOp = LOGOP_AND;
					break;
				case KEY_OR:
					logOp = LOGOP_OR;
					break;
				case KEY_NAND:
					logOp = LOGOP_NAND;
					break;
				case KEY_NOR:
					logOp = LOGOP_NOR;
					break;
				case KEY_XOR:
					logOp = LOGOP_XOR;
					break;
				default:
					break;
			}
		}

		if (logOp != NOLOGOP)
		{
			exp.next = parseMoreRelations(key, logOp);
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
	private Relation parseRelation()
		throws ParseException
	{
		int relOp = NORELOP;
		Relation relation = new Relation();
		relation.simpleExpr = parseSimpleExpression();
		relation.relOperator = NORELOP;
		relation.simpleExpr2 = null;

		switch (nextToken.token)
		{
			case TOKEN_EQ: relOp = RELOP_EQ;   break;
			case TOKEN_NE: relOp = RELOP_NE;   break;
			case TOKEN_LT: relOp = RELOP_LT;   break;
			case TOKEN_LE: relOp = RELOP_LE;   break;
			case TOKEN_GT: relOp = RELOP_GT;   break;
			case TOKEN_GE: relOp = RELOP_GE;   break;
		}

		if (relOp != NORELOP)
		{
			relation.relOperator = relOp;
			getNextToken();
			relation.simpleExpr2 = parseSimpleExpression();
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
	private MRelations parseMoreRelations(int key, int logOp)
		throws ParseException
	{
		MRelations more = null;

		if (isKeySame(nextToken, key))
		{
			getNextToken();
			more = new MRelations();
			more.logOperator = logOp;
			more.relation = parseRelation();
			more.next = parseMoreRelations(key, logOp);
		}
		return more;
	}

	/**
	 * Method to parse a simple expression.
	 * It has the form: simple_expression ::= [sign] term {adding_operator term}
	 * @return the simple expression structure.
	 */
	private SimpleExpr parseSimpleExpression()
		throws ParseException
	{
		SimpleExpr exp = new SimpleExpr();

		// check for optional sign
		if (nextToken.token == TOKEN_PLUS)
		{
			exp.sign = 1;
			getNextToken();
		} else if (nextToken.token == TOKEN_MINUS)
		{
			exp.sign = -1;
			getNextToken();
		} else
		{
			exp.sign = 1;			// default sign
		}

		// next is a term
		exp.term = parseTerm();

		// check for more terms
		exp.next = parseMoreTerms();
		return exp;
	}

	/**
	 * Method to parse a term.
	 * It has the form: term ::= factor {multiplying_operator factor}
	 * @return the term structure.
	 */
	private Term parseTerm()
		throws ParseException
	{
		Term term = new Term();
		term.factor = parseFactor();
		term.next = parseMoreFactors();
		return term;
	}

	/**
	 * Method to parse more factors of a term.
	 * The factors have the form:
	 *     multiplying_operator factor
	 * @return pointer to more factors, null if no more.
	 */
	private MFactors parseMoreFactors()
		throws ParseException
	{
		MFactors more = null;
		int mulOp = NOMULOP;
		if (nextToken.token == TOKEN_STAR)
		{
			mulOp = MULOP_MULTIPLY;
		} else if (nextToken.token == TOKEN_SLASH)
		{
			mulOp = MULOP_DIVIDE;
		} else if (isKeySame(nextToken, KEY_MOD))
		{
			mulOp = MULOP_MOD;
		} else if (isKeySame(nextToken, KEY_REM))
		{
			mulOp = MULOP_REM;
		}
		if (mulOp != NOMULOP)
		{
			getNextToken();
			more = new MFactors();
			more.mulOperator = mulOp;
			more.factor = parseFactor();
			more.next = parseMoreFactors();
		}
		return more;
	}

	/**
	 * Method to parse a factor of the form:
	 *    factor :== primary [** primary] | ABS primary | NOT primary
	 * @return the factor structure.
	 */
	private Factor parseFactor()
		throws ParseException
	{
		Factor factor = null;
		Primary primary = null;
		Primary primary2 = null;
		int miscOp = NOMISCOP;
		if (isKeySame(nextToken, KEY_ABS))
		{
			miscOp = MISCOP_ABS;
			getNextToken();
			primary = parsePrimary();
		} else if (isKeySame(nextToken, KEY_NOT))
		{
			miscOp = MISCOP_NOT;
			getNextToken();
			primary = parsePrimary();
		} else
		{
			primary = parsePrimary();
			if (nextToken.token == TOKEN_DOUBLESTAR)
			{
				miscOp = MISCOP_POWER;
				getNextToken();
				primary2 = parsePrimary();
			}
		}
		factor = new Factor();
		factor.primary = primary;
		factor.miscOperator = miscOp;
		factor.primary2 = primary2;
		return factor;
	}

	/**
	 * Method to parse a primary of the form:
	 *    primary ::= name | literal | aggregate | concatenation | function_call | type_conversion | qualified_expression | (expression)
	 * @return the primary structure.
	 */
	private Primary parsePrimary()
		throws ParseException
	{
		int type = NOPRIMARY;
		Object pointer = null;
		Primary primary = null;
		switch (nextToken.token)
		{
			case TOKEN_DECIMAL:
			case TOKEN_BASED:
			case TOKEN_STRING:
			case TOKEN_BIT_STRING:
				type = PRIMARY_LITERAL;
				pointer = parseLiteral();
				break;
			case TOKEN_IDENTIFIER:
				type = PRIMARY_NAME;
				pointer = parseName();
				break;
			case TOKEN_LEFTBRACKET:
				// should be an expression in brackets
				getNextToken();
				type = PRIMARY_EXPRESSION;
				pointer = parseExpression();

				// should be at right bracket
				if (nextToken.token != TOKEN_RIGHTBRACKET)
				{
					reportErrorMsg(nextToken, "Expecting a right bracket");
				}
				getNextToken();
				break;
			default:
				break;
		}
		if (type != NOPRIMARY)
		{
			primary = new Primary();
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
	private Literal parseLiteral()
		throws ParseException
	{
		Literal literal = null;
		Object pointer = null;
		int type = NOLITERAL;
		switch(nextToken.token)
		{
			case TOKEN_DECIMAL:
				type = LITERAL_NUMERIC;
				pointer = parseDecimal();
				break;
			case TOKEN_BASED:
				// type = LITERAL_NUMERIC;
				// pointer = parseBased();
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
			literal = new Literal();
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
	private Integer parseDecimal()
		throws ParseException
	{
		int value = TextUtils.atoi((String)nextToken.pointer);
		getNextToken();
		return new Integer(value);
	}

	/**
	 * Method to parse more terms of a simple expression.
	 * The terms have the form:
	 *    adding_operator term
	 * @return pointer to more terms, null if no more.
	 */
	private MTerms parseMoreTerms()
		throws ParseException
	{
		MTerms more = null;
		int addOp = NOADDOP;
		if (nextToken.token == TOKEN_PLUS)
		{
			addOp = ADDOP_ADD;
		} else if (nextToken.token == TOKEN_MINUS)
		{
			addOp = ADDOP_SUBTRACT;
		}
		if (addOp != NOADDOP)
		{
			getNextToken();
			more = new MTerms();
			more.addOperator = addOp;
			more.term = parseTerm();
			more.next = parseMoreTerms();
		}
		return more;
	}

	/**
	 * Method to parse an identifier list and return its parse tree.
	 * The form of an identifier list is:
	 *	  identifier_list :== identifier {, identifier}
	 * @return a pointer to identifier list.
	 */
	private IdentList parseIdentList()
		throws ParseException
	{
		// must be at least one identifier
		if (nextToken.token != TOKEN_IDENTIFIER)
		{
			reportErrorMsg(nextToken, "Expecting an identifier");
			getNextToken();
			return null;
		}
		IdentList newIList = new IdentList();
		newIList.identifier = nextToken;
		newIList.next = null;
		IdentList iList = newIList;
		IdentList iListEnd = newIList;

		// continue while a comma is next
		getNextToken();
		while (nextToken.token == TOKEN_COMMA)
		{
			getNextToken();
			// should be another identifier
			if (nextToken.token != TOKEN_IDENTIFIER)
			{
				reportErrorMsg(nextToken, "Expecting an identifier");
				getNextToken();
				return null;
			}
			newIList = new IdentList();
			newIList.identifier = nextToken;
			newIList.next = null;
			iListEnd.next = newIList;
			iListEnd = newIList;
			getNextToken();
		}
		return iList;
	}

	/**
	 * Method to ignore up to the next semicolon.
	 */
	private void parseToSemicolon()
		throws ParseException
	{
		for(;;)
		{
			getNextToken();
			if (nextToken.token == TOKEN_SEMICOLON)
			{
				getNextToken();
				break;
			}
		}
	}

	/**
	 * Method to get the next token if possible.
	 */
	private void getNextToken()
		throws ParseException
	{
		if (nextToken.next == null)
		{
			reportErrorMsg(nextToken, "Unexpected termination within block");
			throw new ParseException();
		}
		nextToken = nextToken.next;
	}

	/**
	 * Method to compare the two keywords, the first as part of a token.
	 * @param tokenPtr pointer to the token entity.
	 * @param key value of key to be compared.
	 * @return true if the same, false if not the same.
	 */
	private boolean isKeySame(TokenList tokenPtr, int key)
	{
		if (tokenPtr.token != TOKEN_KEYWORD) return false;
		if (((VKeyword)(tokenPtr.pointer)).num == key) return true;
		return false;
	}

	private void reportErrorMsg(TokenList tList, String errMsg)
	{
		hasError = true;
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
		TokenList tStart;
		for (tStart = tList; tStart.last != null; tStart = tStart.last)
		{
			if (tStart.last.lineNum != tList.lineNum) break;
		}

		// form line in buffer
		int pointer = 0;
		StringBuffer buffer = new StringBuffer();
		for ( ; tStart != null && tStart.lineNum == tList.lineNum; tStart = tStart.next)
		{
			int i = buffer.length();
			if (tStart == tList) pointer = i;
			if (tStart.token < TOKEN_ARROW)
			{
				char chr = delimiterStr.charAt(tStart.token);
				buffer.append(chr);
			} else if (tStart.token < TOKEN_UNKNOWN)
			{
				int start = 2 * (tStart.token - TOKEN_ARROW);
				buffer.append(doubleDelimiterStr.substring(start, start+2));
			} else switch (tStart.token)
			{
				case TOKEN_STRING:
					buffer.append("\"" + tStart.pointer + "\" ");
					break;
				case TOKEN_KEYWORD:
					buffer.append(((VKeyword)tStart.pointer).name);
					break;
				case TOKEN_IDENTIFIER:
					buffer.append(tStart.pointer);
					break;
				case TOKEN_CHAR:
					buffer.append(((Character)tStart.pointer).charValue());
				case TOKEN_DECIMAL:
					buffer.append(tStart.pointer);
					break;
				default:
					if (tStart.pointer != null)
						buffer.append(tStart.pointer);
					break;
			}
			if (tStart.space) buffer.append(" ");
		}

		// print out line
		System.out.println(buffer.toString());

		// print out pointer
		buffer = new StringBuffer();
		for (int i = 0; i < pointer; i++) buffer.append(" ");
		System.out.println(buffer.toString() + "^");
	}

	/******************************** THE VHDL SEMANTICS ********************************/

	private DBUnits			theUnits;
	private SymbolList		localSymbols, globalSymbols;
	private int             forLoopLevel = 0;
	private int []          forLoopTags = new int[10];

	/**
	 * Method to start semantic analysis of the generated parse tree.
	 * @return the status of the analysis (errors).
	 */
	private boolean doSemantic()
	{
		hasError = false;
		theUnits = new DBUnits();
		theUnits.interfaces = null;
		DBInterface endInterface = null;
		theUnits.bodies = null;
		DBBody endBody = null;

		localSymbols = pushSymbols(null);
		globalSymbols = pushSymbols(null);

		// add defaults to symbol tree
		createDefaultType(localSymbols);
		SymbolList sSymbols = localSymbols;

		localSymbols = pushSymbols(localSymbols);

		for (PTree unit = pTree; unit != null; unit = unit.next)
		{
			switch (unit.type)
			{
				case UNIT_INTERFACE:
					DBInterface interfacef = semInterface((VInterface)unit.pointer);
					if (interfacef == null) break;
					if (endInterface == null)
					{
						theUnits.interfaces = endInterface = interfacef;
					} else
					{
						endInterface.next = interfacef;
						endInterface = interfacef;
					}
					localSymbols = pushSymbols(sSymbols);
					break;
				case UNIT_BODY:
					DBBody body = semBody((Body)unit.pointer);
					if (endBody == null)
					{
						theUnits.bodies = endBody = body;
					} else
					{
						endBody.next = body;
						endBody = body;
					}
					localSymbols = pushSymbols(sSymbols);
					break;
				case UNIT_PACKAGE:
					semPackage((Package)unit.pointer);
					break;
				case UNIT_USE:
					semUse((Use)unit.pointer);
					break;
				case UNIT_FUNCTION:
				default:
					break;
			}
		}

		return hasError;
	}

	/**
	 * Method to do semantic analysis of a use statement.
	 * Add package symbols to symbol list.
	 * @param use pointer to use parse structure.
	 */
	private void semUse(Use use)
	{
		for ( ; use != null; use = use.next)
		{
			/* Note this code was lifted with minor mods from semWith()
			 * which is not a distinct function in IEEE version.
			 * It seems a little redundant as written, but I don't
			 * really understand what Andy was doing here.....
			 */
			SymbolTree symbol = searchSymbol((String)use.unit.pointer, globalSymbols);
			if (symbol == null)
			{
				continue;
			}
			if (symbol.type != SYMBOL_PACKAGE)
			{
				reportErrorMsg(use.unit, "Symbol is not a PACKAGE");
			} else
			{
				addSymbol(symbol.value, SYMBOL_PACKAGE, symbol.pointer, localSymbols);
			}
			symbol = searchSymbol((String)use.unit.pointer, globalSymbols);
			if (symbol == null)
			{
				reportErrorMsg(use.unit, "Symbol is undefined");
				continue;
			}
			if (symbol.type != SYMBOL_PACKAGE)
			{
				reportErrorMsg(use.unit, "Symbol is not a PACKAGE");
			} else
			{
				SymbolList newSymList = ((DBPackage)(symbol.pointer)).root;
				newSymList.last = localSymbols;
				localSymbols = newSymList;
			}
		}
	}

	/**
	 * Method to do semantic analysis of a package declaration.
	 * @param vPackage pointer to a package.
	 */
	private void semPackage(Package vPackage)
	{
		if (vPackage == null) return;
		DBPackage dbPackage = null;

		// search to see if package name is unique
		if (searchSymbol((String)vPackage.name.pointer, globalSymbols) != null)
		{
			reportErrorMsg(vPackage.name, "Symbol previously defined");
		} else
		{
			dbPackage = new DBPackage();
			dbPackage.name = (String)vPackage.name.pointer;
			dbPackage.root = null;
			addSymbol(dbPackage.name, SYMBOL_PACKAGE, dbPackage, globalSymbols);
		}

		// check package parts
		localSymbols = pushSymbols(localSymbols);
		for (PackagedPart part = vPackage.declare; part != null; part = part.next)
		{
			semBasicDeclare(part.item);
		}
		if (dbPackage != null)
		{
			dbPackage.root = localSymbols;
		}
		localSymbols = popSymbols(localSymbols);
	}

	/**
	 * Method to do semantic analysis of an body declaration.
	 * @param body pointer to body parse structure.
	 * @return resultant database body.
	 */
	private DBBody semBody(Body body)
	{
		DBBody dbBody = null;
		if (body == null) return dbBody;
		if (searchSymbol((String)body.name.pointer, globalSymbols) != null)
		{
			reportErrorMsg(body.name, "Body previously defined");
			return dbBody;
		}

		// create dbbody
		dbBody = new DBBody();
		dbBody.name = (String)body.name.pointer;
		dbBody.entity = null;
		dbBody.declare = null;
		dbBody.statements = null;
		dbBody.parent = null;
		dbBody.sameParent = null;
		dbBody.next = null;
		addSymbol(dbBody.name, SYMBOL_BODY, dbBody, globalSymbols);

		// check if interface declared
		SymbolTree symbol = searchSymbol((String)body.entity.identifier.pointer, globalSymbols);
		if (symbol == null)
		{
			reportErrorMsg((TokenList)body.entity.identifier, "Reference to undefined entity");
			return dbBody;
		} else if (symbol.type != SYMBOL_ENTITY)
		{
			reportErrorMsg((TokenList)body.entity.identifier, "Symbol is not an entity");
			return dbBody;
		} else
		{
			dbBody.entity = symbol.value;
			dbBody.parent = (DBInterface)symbol.pointer;
			if (symbol.pointer != null)
			{
				// add interfacef-body reference to list
				dbBody.sameParent = ((DBInterface)(symbol.pointer)).bodies;
				((DBInterface)(symbol.pointer)).bodies = dbBody;
			}
		}

		// create new symbol tree
		SymbolList tempSymbols = localSymbols;
		SymbolList endSymbol = localSymbols;
		if (symbol.pointer != null)
		{
			while (endSymbol.last != null)
			{
				endSymbol = endSymbol.last;
			}
			endSymbol.last = ((DBInterface)(symbol.pointer)).symbols;
		}
		localSymbols = pushSymbols(localSymbols);

		// check body declaration
		dbBody.declare = semBodyDeclare(body.bodyDeclare);

		// check statements
		dbBody.statements = semSetOfStatements(body.statements);

		// delete current symbol table
		localSymbols = tempSymbols;
		endSymbol.last = null;

		return dbBody;
	}

	/**
	 * Method to do semantic analysis of architectural set of statements in a body.
	 * @param state pointer to architectural statements.
	 * @return pointer to created statements.
	 */
	private DBStatements semSetOfStatements(Statements state)
	{
		if (state == null) return null;
		DBStatements dbStates = new DBStatements();
		dbStates.instances = null;
		DBInstance endInstance = null;
		for (; state != null; state = state.next)
		{
			switch (state.type)
			{
				case ARCHSTATE_INSTANCE:
					DBInstance newInstance = semInstance((VInstance)state.pointer);
					if (endInstance == null)
					{
						dbStates.instances = endInstance = newInstance;
					} else
					{
						endInstance.next = newInstance;
						endInstance = newInstance;
					}
					break;
				case ARCHSTATE_GENERATE:
					DBStatements newState = semGenerate((Generate)state.pointer);
					if (newState != null)
					{
						for (newInstance = newState.instances; newInstance != null;
						newInstance = newInstance.next)
						{
							if (endInstance == null)
							{
								dbStates.instances = endInstance = newInstance;
							} else
							{
								endInstance.next = newInstance;
								endInstance = newInstance;
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
		return dbStates;
	}

	/**
	 * Method to do semantic analysis of generate statement.
	 * @param gen pointer to generate statement.
	 * @return pointer to generated statements.
	 */
	private DBStatements semGenerate(Generate gen)
	{
		DBStatements dbStates = null;
		if (gen == null) return dbStates;

		// check label
		// For IEEE standard, check label only if not inside a for generate
		// Not a perfect implementation, but label is not used for anything
		// in this situation.  This is easier to check in the parser...
		if (gen.label != null && forLoopLevel == 0)
		{
			// check label for uniqueness
			if (searchSymbol((String)gen.label.pointer, localSymbols) != null)
			{
				reportErrorMsg(gen.label, "Symbol previously defined");
			} else
			{
				addSymbol((String)gen.label.pointer, SYMBOL_LABEL, null, localSymbols);
			}
		}

		// check generation scheme
		GenScheme scheme = gen.genScheme;
		if (scheme == null) return dbStates;
		switch (scheme.scheme)
		{
			case GENSCHEME_FOR:

				// Increment forLoopLevel and clear tag
				forLoopTags[++forLoopLevel] = 0;

				// create new local symbol table
				localSymbols = pushSymbols(localSymbols);

				// add identifier as a variable symbol
				SymbolTree symbol = addSymbol((String)scheme.identifier.pointer, SYMBOL_VARIABLE,
					null, localSymbols);

				// determine direction of discrete range (ascending or descending)
				DBDiscreteRange dRange = semDiscreteRange(scheme.range);
				if (dRange.start > dRange.end)
				{
					int temp = dRange.end;
					dRange.end = dRange.start;
					dRange.start = temp;
				}
				DBStatements oldStates = null;
				DBInstance endInst = null;
				for (int temp = dRange.start; temp <= dRange.end; temp++)
				{
					symbol.pointer = new Integer(temp);
					dbStates = semSetOfStatements(gen.statements);
					++forLoopTags[forLoopLevel];
					if (dbStates != null)
					{
						if (oldStates == null)
						{
							oldStates = dbStates;
							endInst = dbStates.instances;
							if (endInst != null)
							{
								while (endInst.next != null)
								{
									endInst = endInst.next;
								}
							}
						} else
						{
							for (DBInstance inst = dbStates.instances; inst != null; inst = inst.next)
							{
								if (endInst == null)
								{
									oldStates.instances = endInst = inst;
								} else
								{
									endInst.next = inst;
									endInst = inst;
								}
							}
						}
					}
				}
				dbStates = oldStates;

				// restore old symbol table
				localSymbols = popSymbols(localSymbols);
				--forLoopLevel;
				break;

			case GENSCHEME_IF:
				if (evalExpression(scheme.condition) != 0)
				{
					dbStates = semSetOfStatements(gen.statements);
				}
			default:
				break;
		}
		return dbStates;
	}

	/**
	 * Method to do demantic analysis of instance for an architectural body.
	 * @param inst pointer to instance structure.
	 * @return pointer to created instance.
	 */
	private DBInstance semInstance(VInstance inst)
	{
		DBInstance dbInst = null;
		if (inst == null) return dbInst;
		// If inside a "for generate" make unique instance name
		// from instance label and forLoopTags[]

		String iKey = null;
		if (forLoopLevel > 0)
		{
			if (inst.name == null) iKey = "no_name"; else
				iKey = (String)inst.name.pointer;
			for (int i=1; i<=forLoopLevel; ++i)
			{
				iKey += "_" + forLoopTags[i];
			}
		} else
		{
			iKey = (String)inst.name.pointer;
		}
		dbInst = new DBInstance();
		dbInst.name = iKey;
		dbInst.compo = null;
		dbInst.ports = null;
		DBAPortList endDBAPort = null;
		dbInst.next = null;
		if (searchSymbol(dbInst.name, localSymbols) != null)
		{
			reportErrorMsg(inst.name, "Instance name previously defined");
		} else
		{
			addSymbol(dbInst.name, SYMBOL_INSTANCE, dbInst, localSymbols);
		}

		// check that instance entity is among component list
		DBComponents compo = null;
		SymbolTree symbol = searchSymbol((String)inst.entity.identifier.pointer, localSymbols);
		if (symbol == null)
		{
			reportErrorMsg(inst.entity.identifier, "Instance references undefined component");
		} else if (symbol.type != SYMBOL_COMPONENT)
		{
			reportErrorMsg(inst.entity.identifier, "Symbol is not a component reference");
		} else
		{
			compo = (DBComponents)symbol.pointer;
			dbInst.compo = compo;

			// check that number of ports match
			int iPortNum = 0;
			for (APortList apList = inst.ports; apList != null; apList = apList.next)
			{
				iPortNum++;
			}
			int cPortNum = 0;
			for (DBPortList pList = compo.ports; pList != null; pList = pList.next)
			{
				cPortNum++;
			}
			if (iPortNum != cPortNum)
			{
				reportErrorMsg(getNameToken((VName)inst.ports.pointer),
					"Instance has different number of ports that component");
				return null;
			}
		}

		// check that ports of instance are either signals or entity port
		// note 0 ports are allowed for position placement
		DBPortList pList = null;
		if (compo != null)
		{
			pList = compo.ports;
		}
		for (APortList apList = inst.ports; apList != null; apList = apList.next)
		{
			DBAPortList dbAPort = new DBAPortList();
			dbAPort.name = null;
			dbAPort.port = pList;
			if (pList != null)
			{
				pList = pList.next;
			}
			dbAPort.flags = 0;
			dbAPort.next = null;
			if (endDBAPort == null)
			{
				dbInst.ports = endDBAPort = dbAPort;
			} else
			{
				endDBAPort.next = dbAPort;
				endDBAPort = dbAPort;
			}
			if (apList.pointer == null) continue;
			dbAPort.name = semName((VName)apList.pointer);

			// check that name is reference to a signal or formal port
			semAPortCheck((VName)apList.pointer);
		}

		return dbInst;
	}

	/**
	 * Method to do semantic analysis of a name.
	 * @param name pointer to name structure.
	 * @return pointer to created db name.
	 */
	private DBName semName(VName name)
	{
		DBName dbName = null;
		if (name == null) return dbName;
		switch (name.type)
		{
			case NAME_SINGLE:
				dbName = semSingleName((SingleName)name.pointer);
				break;
			case NAME_CONCATENATE:
				dbName = semConcatenatedName((ConcatenatedName)name.pointer);
				break;
			case NAME_ATTRIBUTE:
			default:
				break;
		}
		return dbName;
	}

	/**
	 * Method to do semantic analysis of a concatenated name.
	 * @param name pointer to concatenated name structure.
	 * @return pointer to generated db name.
	 */
	private DBName semConcatenatedName(ConcatenatedName name)
	{
		DBName dbName = null;
		if (name == null) return dbName;
		dbName = new DBName();
		dbName.name = null;
		dbName.type = DBNAME_CONCATENATED;
		dbName.pointer = null;
		dbName.dbType = null;
		DBNameList end = null;
		for (ConcatenatedName cat = name; cat != null; cat = cat.next)
		{
			DBNameList newNL = new DBNameList();
			newNL.name = semSingleName(cat.name);
			newNL.next = null;
			if (end != null)
			{
				end.next = newNL;
				end = newNL;
			} else
			{
				end = newNL;
				dbName.pointer = newNL;
			}
		}
		return dbName;
	}

	/**
	 * Method to do semantic analysis of a single name.
	 * @param name pointer to single name structure.
	 * @return pointer to generated db name.
	 */
	private DBName semSingleName(SingleName name)
	{
		DBName dbName = null;
		if (name == null) return dbName;
		switch (name.type)
		{
			case SINGLENAME_SIMPLE:
				dbName = new DBName();
				dbName.name = (String)((SimpleName)(name.pointer)).identifier.pointer;
				dbName.type = DBNAME_IDENTIFIER;
				dbName.pointer = null;
				dbName.dbType = getType(dbName.name);
				break;
			case SINGLENAME_INDEXED:
				dbName = semIndexedName((IndexedName)name.pointer);
				break;
			case SINGLENAME_SLICE:
			case SINGLENAME_SELECTED:
			default:
				break;
		}
		return dbName;
	}

	/**
	 * Method to do semantic analysis of an indexed name.
	 * @param name pointer to indexed name structure.
	 * @return pointer to generated name.
	 */
	private DBName semIndexedName(IndexedName name)
	{
		DBName dbName = null;
		if (name == null) return dbName;

		// must be an array type
		DBLType type = getType(getPrefixIdent(name.prefix));
		if (type == null)
		{

			reportErrorMsg(getPrefixToken(name.prefix), "No type specified");
			return dbName;
		}
		if (type.type != DBTYPE_ARRAY)
		{
			reportErrorMsg(getPrefixToken(name.prefix), "Must be of constrained array type");
			return dbName;
		}
		dbName = new DBName();
		dbName.name = getPrefixIdent(name.prefix);
		dbName.type = DBNAME_INDEXED;
		dbName.pointer = null;
		dbName.dbType = type;

		// evaluate any expressions
		DBIndexRange indexR = (DBIndexRange)type.pointer;
		DBExprList dbExpr = null, endExpr = null;
		for (ExprList expr = name.exprList; expr != null && indexR != null; expr = expr.next)
		{
			int value = evalExpression(expr.expression);
			if (!isInDiscreteRange(value, indexR.dRange))
			{
				reportErrorMsg(getPrefixToken(name.prefix), "Index is out of range");
				return dbName;
			}
			DBExprList nExpr = new DBExprList();
			nExpr.value = value;
			nExpr.next = null;
			if (endExpr == null)
			{
				dbExpr = endExpr = nExpr;
			} else
			{
				endExpr.next = nExpr;
				endExpr = nExpr;
			}
			indexR = indexR.next;
		}
		dbName.pointer = dbExpr;
		return dbName;
	}

	/**
	 * Method to decide whether value is in discrete range.
	 * @param value value to be checked.
	 * @param discrete pointer to db discrete range structure.
	 * @return true if value in discrete range, else false.
	 */
	private boolean isInDiscreteRange(int value, DBDiscreteRange discrete)
	{
		boolean inRange = false;
		if (discrete == null)
			return inRange;
		int start = discrete.start;
		int end = discrete.end;
		if (start > end)
		{
			int temp = end;
			end = start;
			start = temp;
		}
		if (value >= start && value <= end)
			inRange = true;
		return inRange;
	}

	/**
	 * Method to get the type of an identifier.
	 * @param ident pointer to identifier.
	 * @return type, null if no type.
	 */
	private DBLType getType(String ident)
	{
		DBLType type = null;
		if (ident != null)
		{
			SymbolTree symbol = searchSymbol(ident, localSymbols);
			if (symbol != null)
			{
				type = getSymbolType(symbol);
			}
		}

		return type;
	}

	/**
	 * Method to get a pointer to the type of a symbol.
	 * @param symbol pointer to symbol.
	 * @return pointer to returned type, null if no type exists.
	 */
	private DBLType getSymbolType(SymbolTree symbol)
	{
		DBLType type = null;
		if (symbol == null) return type;
		switch (symbol.type)
		{
			case SYMBOL_FPORT:
				DBPortList fPort = (DBPortList)symbol.pointer;
				if (fPort == null) break;
				type = fPort.type;
				break;
			case SYMBOL_SIGNAL:
				DBSignals signal = (DBSignals)symbol.pointer;
				if (signal == null) break;
				type = signal.type;
				break;
			case SYMBOL_TYPE:
				type = (DBLType)symbol.pointer;
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
	private void semAPortCheck(VName name)
	{
		switch (name.type)
		{
			case NAME_SINGLE:
				semAPortCheckSingleName((SingleName)name.pointer);
				break;
			case NAME_CONCATENATE:
				for (ConcatenatedName cat = (ConcatenatedName)name.pointer; cat != null; cat = cat.next)
				{
					semAPortCheckSingleName(cat.name);
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Method to check that the passed single name references a signal or formal port.
	 * @param sName pointer to single name structure.
	 */
	private void semAPortCheckSingleName(SingleName sName)
	{
		switch (sName.type)
		{
			case SINGLENAME_SIMPLE:
				SimpleName simName = (SimpleName)sName.pointer;
				String ident = (String)simName.identifier.pointer;
				SymbolTree symbol = searchSymbol(ident, localSymbols);
				if (symbol == null ||
					(symbol.type != SYMBOL_FPORT && symbol.type != SYMBOL_SIGNAL))
				{
					reportErrorMsg(simName.identifier,
						"Instance port has reference to unknown port");
				}
				break;
			case SINGLENAME_INDEXED:
				IndexedName iName = (IndexedName)sName.pointer;
				ident = getPrefixIdent(iName.prefix);
				symbol = searchSymbol(ident, localSymbols);
				if (symbol == null)
				{
					symbol = searchSymbol(ident, localSymbols);
				}
				if (symbol == null ||
					(symbol.type != SYMBOL_FPORT && symbol.type != SYMBOL_SIGNAL))
				{
					reportErrorMsg(getPrefixToken(iName.prefix),
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
	private DBBodyDelcare semBodyDeclare(BodyDeclare declare)
	{
		DBBodyDelcare dbDeclare = null;
		if (declare == null) return dbDeclare;
		dbDeclare = new DBBodyDelcare();
		dbDeclare.components = null;
		DBComponents endComponent = null;
		dbDeclare.bodySignals = null;
		DBSignals endSignal = null;

		for (; declare != null; declare = declare.next)
		{
			switch (declare.type)
			{
				case BODYDECLARE_BASIC:
					DBSignals newSignals = semBasicDeclare((BasicDeclare)declare.pointer);
					if (newSignals != null)
					{
						if (endSignal == null)
						{
							dbDeclare.bodySignals = endSignal = newSignals;
						} else
						{
							endSignal.next = newSignals;
							endSignal = newSignals;
						}
						while (endSignal.next != null)
						{
							endSignal = endSignal.next;
						}
					}
					break;
				case BODYDECLARE_COMPONENT:
					DBComponents newComponent = semComponent((VComponent)declare.pointer);
					if (newComponent != null)
					{
						if (endComponent == null)
						{
							dbDeclare.components = endComponent = newComponent;
						} else
						{
							endComponent.next = newComponent;
							endComponent = newComponent;
						}
					}
					break;
				case BODYDECLARE_RESOLUTION:
				case BODYDECLARE_LOCAL:
				default:
					break;
			}
		}

		return dbDeclare;
	}

	/**
	 * Method to do semantic analysis of body's component.
	 * @param compo pointer to component parse.
	 * @return pointer to created component.
	 */
	private DBComponents semComponent(VComponent compo)
	{
		DBComponents dbComp = null;
		if (compo == null) return dbComp;
		if (searchFSymbol((String)compo.name.pointer, localSymbols) != null)
		{
			reportErrorMsg(compo.name, "Identifier previously defined");
			return dbComp;
		}
		dbComp = new DBComponents();
		dbComp.name = (String)compo.name.pointer;
		dbComp.ports = null;
		dbComp.next = null;
		addSymbol(dbComp.name, SYMBOL_COMPONENT, dbComp, localSymbols);
		localSymbols = pushSymbols(localSymbols);
		dbComp.ports = semFormalPortList(compo.ports);
		localSymbols = popSymbols(localSymbols);
		return dbComp;
	}

	/**
	 * Method to top off the top most symbol list and return next symbol list.
	 * @param oldSymList pointer to old symbol list.
	 * @return pointer to new symbol list.
	 */
	private SymbolList popSymbols(SymbolList oldSymList)
	{
		if (oldSymList == null)
		{
			System.out.println("ERROR - trying to pop nonexistant symbol list.");
			return null;
		}
		SymbolList newSymList = oldSymList.last;
		return newSymList;
	}

	/**
	 * Method to do semantic analysis of basic declaration.
	 * @param declare pointer to basic declaration structure.
	 * @return pointer to new signal, null if not.
	 */
	private DBSignals semBasicDeclare(BasicDeclare declare)
	{
		DBSignals dbSignal = null;
		if (declare == null) return dbSignal;
		switch (declare.type)
		{
			case BASICDECLARE_OBJECT:
				dbSignal = semObjectDeclare((ObjectDeclare)declare.pointer);
				break;
			case BASICDECLARE_TYPE:
				semTypeDeclare((Type)declare.pointer);
				break;
			case BASICDECLARE_SUBTYPE:
			case BASICDECLARE_CONVERSION:
			case BASICDECLARE_ATTRIBUTE:
			case BASICDECLARE_ATT_SPEC:
			default:
				break;
		}
		return dbSignal;
	}

	/**
	 * Method to do semantic analysis of a type declaration.
	 * @param type pointer to type parse tree.
	 */
	private void semTypeDeclare(Type type)
	{
		DBLType dbType = null;
		if (type == null) return;

		// check that type name is distict
		if (searchSymbol((String)type.identifier.pointer, localSymbols) != null)
		{
			reportErrorMsg(type.identifier, "Identifier previously defined");
			return;
		}

		// check type definition
		switch (type.type)
		{
			case TYPE_SCALAR:
				break;
			case TYPE_COMPOSITE:
				dbType = semCompositeType((Composite)type.pointer);
				break;
			default:
				break;
		}

		// add symbol to list
		if (dbType != null)
		{
			dbType.name = (String)type.identifier.pointer;
			addSymbol(dbType.name, SYMBOL_TYPE, dbType, localSymbols);
		}
	}

	/**
	 * Method to do semantic analysis of a composite type definition.
	 * @param composite pointer to composite type structure.
	 * @return generated db type.
	 */
	private DBLType semCompositeType(Composite composite)
	{
		DBLType dbType = null;
		if (composite == null) return dbType;
		switch (composite.type)
		{
			case COMPOSITE_ARRAY:
				dbType = semArrayType((Array)composite.pointer);
				break;
			case COMPOSITE_RECORD:
				break;
			default:
				break;
		}
		return dbType;
	}

	/**
	 * Method to do semantic analysis of an array composite type definition.
	 * @param array pointer to composite array type structure.
	 * @return pointer to generated type.
	 */
	private DBLType semArrayType(Array array)
	{
		DBLType dbType = null;
		if (array == null) return dbType;
		switch (array.type)
		{
			case ARRAY_UNCONSTRAINED:
				break;
			case ARRAY_CONSTRAINED:
				dbType = semConstrainedArray((Constrained)array.pointer);
				break;
			default:
				break;
		}
		return dbType;
	}

	/**
	 * Method to do semantic analysis of a composite constrained array type definition.
	 * @param constr pointer to constrained array structure.
	 * @return pointer to generated type.
	 */
	private DBLType semConstrainedArray(Constrained constr)
	{
		DBLType dbType = null;
		if (constr == null) return dbType;
		dbType = new DBLType();
		dbType.name = null;
		dbType.type = DBTYPE_ARRAY;
		DBIndexRange endRange = null;
		dbType.pointer = null;
		dbType.subType = null;

		// check index constraint
		for (IndexConstraint indexC = constr.constraint; indexC != null; indexC = indexC.next)
		{
			DBIndexRange newRange = new DBIndexRange();
			newRange.dRange = semDiscreteRange(indexC.discrete);
			newRange.next = null;
			if (endRange == null)
			{
				endRange = newRange;
				dbType.pointer = newRange;
			} else
			{
				endRange.next = newRange;
				endRange = newRange;
			}
		}
		// check subtype indication
		dbType.subType = semSubtypeIndication(constr.subType);

		return dbType;
	}

	/**
	 * Method to do semantic analysis of a sybtype indication.
	 * @param subtype pointer to subtype indication.
	 * @return pointer to db type;
	 */
	private DBLType semSubtypeIndication(SubTypeInd subType)
	{
		DBLType dbType = null;
		if (subType == null) return dbType;
		dbType = semTypeMark(subType.type);
		return dbType;
	}

	/**
	 * Method to do semantic type mark.
	 * @param name pointer to type name.
	 * @return pointer to db type.
	 */
	private DBLType semTypeMark(VName name)
	{
		DBLType dbType = null;
		if (name == null) return dbType;
		SymbolTree symbol = searchSymbol(getNameIdent(name), localSymbols);
		if (symbol == null ||
			symbol.type != SYMBOL_TYPE)
		{
			reportErrorMsg(getNameToken(name), "Bad type");
		} else
		{
			dbType = (DBLType)symbol.pointer;
		}
		return dbType;
	}

	/**
	 * Method to do semantic analysis of a discrete range.
	 * @param discrete pointer to a discrete range structure.
	 * @return pointer to generated range.
	 */
	private DBDiscreteRange semDiscreteRange(DiscreteRange discrete)
	{
		DBDiscreteRange dbRange = null;
		if (discrete == null) return dbRange;
		switch (discrete.type)
		{
			case DISCRETERANGE_SUBTYPE:
				break;
			case DISCRETERANGE_RANGE:
				dbRange = semRange((Range)discrete.pointer);
				break;
			default:
				break;
		}
		return dbRange;
	}

	/**
	 * Method to do semantic analysis of a range.
	 * @param range pointer to a range structure.
	 * @return pointer to generated range.
	 */
	private DBDiscreteRange semRange(Range range)
	{
		DBDiscreteRange dbRange = null;
		if (range == null) return dbRange;
		switch (range.type)
		{
			case RANGE_ATTRIBUTE:
				break;
			case RANGE_SIMPLE_EXPR:
				RangeSimple rSimp = (RangeSimple)range.pointer;
				if (rSimp != null)
				{
					dbRange = new DBDiscreteRange();
					dbRange.start = evalSimpleExpr(rSimp.start);
					dbRange.end = evalSimpleExpr(rSimp.end);
				}
				break;
			default:
				break;
		}
		return dbRange;
	}

	/**
	 * Method to do semantic analysis of object declaration.
	 * @param primary pointer to object declaration structure.
	 * @return pointer to new signals, null if not.
	 */
	private DBSignals semObjectDeclare(ObjectDeclare declare)
	{
		DBSignals signals = null;
		if (declare == null) return signals;
		switch (declare.type)
		{
			case OBJECTDECLARE_SIGNAL:
				signals = semSignalDeclare((SignalDeclare)declare.pointer);
				break;
			case OBJECTDECLARE_CONSTANT:
				semConstantDeclare((ConstantDeclare)declare.pointer);
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
	private void semConstantDeclare(ConstantDeclare constant)
	{
		if (constant == null) return;

		// check if name exists in top level of symbol tree
		if (searchFSymbol((String)constant.identifier.pointer, localSymbols) != null)
		{
			reportErrorMsg(constant.identifier, "Symbol previously defined");
		} else
		{
			int value = evalExpression(constant.expression);
			addSymbol((String)constant.identifier.pointer, SYMBOL_CONSTANT,
				new Integer(value), localSymbols);
		}
	}

	/**
	 * Method to get the value of an expression.
	 * @param expr pointer to expression structure.
	 * @return value.
	 */
	private int evalExpression(Expression expr)
	{
		if (expr == null) return 0;
		int value = evalRelation(expr.relation);
		if (expr.next != null)
		{
			if (value != 0) value = 1;
		}
		for (MRelations more = expr.next; more != null; more = more.next)
		{
			int value2 = evalRelation(more.relation);
			if (value2 != 0) value2 = 1;
			switch (more.logOperator)
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
	private int evalRelation(Relation relation)
	{
		if (relation == null) return 0;
		int value = evalSimpleExpr(relation.simpleExpr);
		if (relation.relOperator != NORELOP)
		{
			int value2 = evalSimpleExpr(relation.simpleExpr2);
			switch (relation.relOperator)
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
	private int evalSimpleExpr(SimpleExpr expr)
	{
		if (expr == null) return 0;
		int value = evalTerm(expr.term) * expr.sign;
		for (MTerms more = expr.next; more != null; more = more.next)
		{
			int value2 = evalTerm(more.term);
			switch (more.addOperator)
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
	private int evalTerm(Term term)
	{
		if (term == null) return 0;
		int value = evalFactor(term.factor);
		for (MFactors more = term.next; more != null; more = more.next)
		{
			int value2 = evalFactor(more.factor);
			switch (more.mulOperator)
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
	private int evalFactor(Factor factor)
	{
		if (factor == null) return 0;
		int value = evalPrimary(factor.primary);
		switch (factor.miscOperator)
		{
			case MISCOP_POWER:
				int value2 = evalPrimary(factor.primary2);
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
	private int evalPrimary(Primary primary)
	{
		if (primary == null) return 0;
		int value = 0;
		switch (primary.type)
		{
			case PRIMARY_LITERAL:
				Literal literal = (Literal)primary.pointer;
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
				value = evalName((VName)primary.pointer);
				break;
			case PRIMARY_EXPRESSION:
				value = evalExpression((Expression)primary.pointer);
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
	private int evalName(VName name)
	{
		if (name == null) return 0;
		int value = 0;
		SymbolTree symbol = searchSymbol(getNameIdent(name), localSymbols);
		if (symbol == null)
		{
			reportErrorMsg(getNameToken(name), "Symbol is undefined");
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
			reportErrorMsg(getNameToken(name), "Cannot evaluate value of symbol");
			return value;
		}
		return value;
	}

	/**
	 * Method to do semantic analysis of signal declaration.
	 * @param signal pointer to signal declaration.
	 * @return pointer to new signals.
	 */
	private DBSignals semSignalDeclare(SignalDeclare signal)
	{
		DBSignals signals = null;
		if (signal == null) return signals;

		// check for valid type
		String type = getNameIdent(signal.subType.type);
		SymbolTree symbol = searchSymbol(type, localSymbols);
		if (symbol == null || symbol.type != SYMBOL_TYPE)
		{
			reportErrorMsg(getNameToken(signal.subType.type), "Bad type");
		}

		// check each signal in signal list for uniqueness
		for (IdentList sig = signal.names; sig != null; sig = sig.next)
		{
			if (searchSymbol((String)sig.identifier.pointer, localSymbols) != null)
			{
				reportErrorMsg(sig.identifier, "Signal previously defined");
			} else
			{
				DBSignals newSignal = new DBSignals();
				newSignal.name = (String)sig.identifier.pointer;
				if (symbol != null)
				{
					newSignal.type = (DBLType)symbol.pointer;
				} else
				{
					newSignal.type = null;
				}
				newSignal.next = signals;
				signals = newSignal;
				addSymbol(newSignal.name, SYMBOL_SIGNAL, newSignal, localSymbols);
			}
		}

		return signals;
	}

	/**
	 * Method to do semantic analysis of an interface declaration.
	 * @param interfacef pointer to interface parse structure.
	 * @return resultant database interface.
	 */
	private DBInterface semInterface(VInterface interfacef)
	{
		DBInterface dbInter = null;
		if (interfacef == null) return dbInter;
		if (searchSymbol((String)interfacef.name.pointer, globalSymbols) != null)
		{
			reportErrorMsg(interfacef.name, "Entity previously defined");
		} else
		{
			dbInter = new DBInterface();
			dbInter.name = (String)interfacef.name.pointer;
			dbInter.ports = null;
			dbInter.flags = 0;
			dbInter.bodies = null;
			dbInter.symbols = null;
			dbInter.next = null;
			addSymbol(dbInter.name, SYMBOL_ENTITY, dbInter, globalSymbols);
			localSymbols = pushSymbols(localSymbols);
			dbInter.ports = semFormalPortList(interfacef.ports);

			// remove last symbol tree
			SymbolList endSymbol = localSymbols;
			while (endSymbol.last.last != null)
			{
				endSymbol = endSymbol.last;
			}
			endSymbol.last = null;
			dbInter.symbols = localSymbols;
		}
		return dbInter;
	}

	/**
	 * Method to check the semantic of the passed formal port list.
	 * @param port pointer to start of formal port list.
	 * @return pointer to database port list.
	 */
	private DBPortList semFormalPortList(FPortList port)
	{
		DBPortList dbPorts = null;
		DBPortList endPort = null;

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
					reportErrorMsg(port.names.identifier, "Unknown port mode");
					break;
			}

			// check the type
			String symName = getNameIdent(port.type);
			SymbolTree symbol = searchSymbol(symName, localSymbols);
			if (symbol == null || symbol.type != SYMBOL_TYPE)
			{
				reportErrorMsg(getNameToken(port.type), "Unknown port name (" + symName + ")");
			}

			// check for uniqueness of port names
			for (IdentList names = port.names; names != null; names = names.next)
			{
				if (searchFSymbol((String)names.identifier.pointer, localSymbols) != null)
				{
					reportErrorMsg(names.identifier, "Duplicate port name in port list");
				} else
				{
					// add to port list
					DBPortList newPort = new DBPortList();
					newPort.name = (String)names.identifier.pointer;
					newPort.mode = port.mode;
					if (symbol != null)
					{
						newPort.type = (DBLType)symbol.pointer;
					} else
					{
						newPort.type = null;
					}
					newPort.flags = 0;
					newPort.next = null;
					if (endPort == null)
					{
						dbPorts = endPort = newPort;
					} else
					{
						endPort.next = newPort;
						endPort = newPort;
					}
					addSymbol(newPort.name, SYMBOL_FPORT, newPort, localSymbols);
				}
			}
		}

		return dbPorts;
	}

	/**
	 * Method to find a reference to a token given a pointer to a name.
	 * @param name pointer to name structure.
	 * @return pointer to token, null if not found.
	 */
	private TokenList getNameToken(VName name)
	{
		TokenList token = null;
		if (name == null) return token;
		switch (name.type)
		{
			case NAME_SINGLE:
				SingleName singl = (SingleName)(name.pointer);
				switch (singl.type)
				{
					case SINGLENAME_SIMPLE:
						token = ((SimpleName)singl.pointer).identifier;
						break;
					case SINGLENAME_SELECTED:
						break;
					case SINGLENAME_INDEXED:
						token = getPrefixToken(((IndexedName)(singl.pointer)).prefix);
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
	private TokenList getPrefixToken(Prefix prefix)
	{
		TokenList token = null;
		if (prefix == null) return token;
		switch (prefix.type)
		{
			case PREFIX_NAME:
				token = getNameToken((VName)prefix.pointer);
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
	 * @param symList pointer to last (current) symbol list.
	 * @return a pointer to the node, if not found, return null.
	 */
	private SymbolTree searchSymbol(String ident, SymbolList symList)
	{
		String lcIdent = TextUtils.canonicString(ident);
		for ( ; symList != null; symList = symList.last)
		{
			SymbolTree node = symList.sym.get(lcIdent);
			if (node != null) return node;
		}
		return null;
	}

	/**
	 * Method to search the symbol list for the first symbol of the passed value.
	 * Note that only the first symbol tree of the list is checked.
	 * @param ident global name.
	 * @param symList pointer to last (current) symbol list.
	 * @return a pointer to the node, if not found, return null.
	 */
	private SymbolTree searchFSymbol(String ident, SymbolList symList)
	{
		if (symList != null)
		{
			SymbolTree node = symList.sym.get(TextUtils.canonicString(ident));
			if (node != null) return node;
		}
		return null;
	}

	/**
	 * Method to get a name given a pointer to a name.
	 * @param name pointer to name structure.
	 * @return global name.
	 */
	private String getNameIdent(VName name)
	{
		String iTable = null;
		if (name == null) return iTable;
		switch (name.type)
		{
			case NAME_SINGLE:
				SingleName singl = (SingleName)name.pointer;
				switch (singl.type)
				{
					case SINGLENAME_SIMPLE:
						iTable = (String)((SimpleName)singl.pointer).identifier.pointer;
						break;
					case SINGLENAME_INDEXED:
						iTable = getPrefixIdent(((IndexedName)(singl.pointer)).prefix);
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
		return iTable;
	}

	/**
	 * Method to get a name given a pointer to a prefix.
	 * @param prefix pointer to prefix structure.
	 * @return string identifier.
	 */
	private String getPrefixIdent(Prefix prefix)
	{
		String iTable = null;
		if (prefix == null) return iTable;
		switch (prefix.type)
		{
			case PREFIX_NAME:
				iTable = getNameIdent((VName)prefix.pointer);
				break;
			case PREFIX_FUNCTION_CALL:
			default:
				break;
		}
		return iTable;
	}

	/**
	 * Method to create the default type symbol tree.
	 * @param symbols pointer to current symbol list.
	 */
	private void createDefaultType(SymbolList symbols)
	{
		// type BIT
		identTable.add("BIT");
		addSymbol("BIT", SYMBOL_TYPE, null, symbols);

		// type "std_logic"
		identTable.add("std_logic");
		addSymbol("std_logic", SYMBOL_TYPE, null, symbols);
	}

	/**
	 * Method to add a symbol to the symbol tree at the current symbol list.
	 * @param value pointer to identifier in namespace.
	 * @param type type of symbol.
	 * @param pointer generic pointer to symbol.
	 * @param symList pointer to symbol list.
	 * @return pointer to created symbol.
	 */
	private SymbolTree addSymbol(String value, int type, Object pointer, SymbolList symList)
	{
		SymbolTree symbol = new SymbolTree();
		symbol.value = value;
		symbol.type = type;
		symbol.pointer = pointer;
		symList.sym.put(TextUtils.canonicString(value), symbol);
		return symbol;
	}

	/**
	 * Method to add a new symbol tree to the symbol list.
	 * @param oldSymList pointer to old symbol list.
	 * @return the new symbol list.
	 */
	private SymbolList pushSymbols(SymbolList oldSymList)
	{
		SymbolList newSymList = new SymbolList();
		newSymList.sym = new HashMap<String,SymbolTree>();

		newSymList.last = oldSymList;

		return newSymList;
	}

	/******************************** THE ALS NETLIST GENERATOR ********************************/

	private static String [] power =
	{
		"gate power(p)",
		"set p=H@3",
		"t: delta=0"
	};
	private static String [] ground =
	{
		"gate ground(g)",
		"set g=L@3",
		"t: delta=0"
	};
	private static String [] pMOStran =
	{
		"function PMOStran(g, a1, a2)",
		"i: g, a1, a2",
		"o: a1, a2",
		"t: delta=1e-8"
	};
	private static String [] pMOStranWeak =
	{
		"function pMOStranWeak(g, a1, a2)",
		"i: g, a1, a2",
		"o: a1, a2",
		"t: delta=1e-8"
	};
	private static String [] nMOStran =
	{
		"function nMOStran(g, a1, a2)",
		"i: g, a1, a2",
		"o: a1, a2",
		"t: delta=1e-8"
	};
	private static String [] nMOStranWeak =
	{
		"function nMOStranWeak(g, a1, a2)",
		"i: g, a1, a2",
		"o: a1, a2",
		"t: delta=1e-8"
	};
	private static String [] inverter =
	{
		"gate inverter(a,z)",
		"t: delta=1.33e-9",		/* T3=1.33 */
		"i: a=L o: z=H",
		"t: delta=1.07e-9",		/* T1=1.07 */
		"i: a=H o: z=L",
		"t: delta=0",
		"i: a=X o: z=X",
		"load: a=1.0"
	};
	private static String [] buffer =
	{
		"gate buffer(in,out)",
		"t: delta=0.56e-9",		/* T4*Cin=0.56 */
		"i: in=H o: out=H",
		"t: delta=0.41e-9",		/* T2*Cin=0.41 */
		"i: in=L o: out=L",
		"t: delta=0",
		"i: in=X o: out=X"
	};
	private static String [] xor2 =
	{
		/* input a,b cap = 0.xx pF, Tphl = T1 + T2*Cin, Tplh = T3 + T4*Cin */
		"model xor2(a,b,z)",
		"g1: xor2fun(a,b,out)",
		"g2: xor2buf(out,z)",

		"gate xor2fun(a,b,out)",
		"t: delta=1.33e-9",		/* T3=1.33 */
		"i: a=L b=H o: out=H",
		"i: a=H b=L o: out=H",
		"t: delta=1.07e-9",		/* T1=1.07 */
		"i: a=L b=L o: out=L",
		"i: a=H b=H o: out=L",
		"t: delta=0",
		"i:         o: out=X",
		"load: a=1.0 b=1.0",

		"gate xor2buf(in,out)",
		"t: delta=0.56e-9",		/* T4*Cin=0.56 */
		"i: in=H    o: out=H",
		"t: delta=0.41e-9",		/* T2*Cin=0.41 */
		"i: in=L    o: out=L",
		"t: delta=0",
		"i: in=X    o: out=X"
	};
	private static String [] JKFF =
	{
		"model jkff(j, k, clk, pr, clr, q, qbar)",
		"n: JKFFLOP(clk, j, k, q, qbar)",
		"function JKFFLOP(clk, j, k, q, qbar)",
		"i: clk, j, k",
		"o: q, qbar",
		"t: delta=1e-8"
	};
	private static String [] DFF =
	{
		"model dsff(d, clk, pr, q)",
		"n: DFFLOP(d, clk, q)",
		"function DFFLOP(d, clk, q)",
		"i: d, clk",
		"o: q",
		"t: delta=1e-8"
	};

	/**
	 * Method to generate ALS target output for the created parse tree.
	 * Assume parse tree is semantically correct.
     * @param destLib destination library.
     * @param behaveLib behaviour library.
	 * @return a list of strings that has the netlist.
	 */
	private List<String> genALS(Library destLib, Library behaveLib)
	{
		Cell basenp = vhdlCell;

		// print file header
		List<String> netlist = new ArrayList<String>();
		netlist.add("#*************************************************");
		netlist.add("#  ALS Netlist file");
		netlist.add("#");
		if (User.isIncludeDateAndVersionInOutput())
			netlist.add("#  File Creation:    " + TextUtils.formatDate(new Date()));
		netlist.add("#-------------------------------------------------");
		netlist.add("");

		// determine top level cell
		DBInterface topInterface = findTopInterface(theUnits);
		if (topInterface == null)
			System.out.println("ERROR - Cannot find interface to rename main."); else
		{
			// clear written flag on all entities
			for (DBInterface interfacef = theUnits.interfaces; interfacef != null;
				interfacef = interfacef.next) interfacef.flags &= ~ENTITY_WRITTEN;
			genALSInterface(topInterface, basenp.getName(), netlist);
		}

		// print closing line of output file
		netlist.add("#********* End of netlist *******************");

		// scan unresolved references for reality inside of Electric
		int total = 0;
		for (UnResList uList = unResolvedList; uList != null; uList = uList.next)
		{
			total++;
			String gate = uList.interfacef;

			// first see if this is a reference to a cell in the destination library
			if (addNetlist(destLib, gate, netlist))
			{
				uList.numRef = 0;
				total--;
				continue;
			}

			// next see if this is a reference to the behavior library
			if (behaveLib != null && behaveLib != destLib)
			{
				if (addNetlist(behaveLib, gate, netlist))
				{
					uList.numRef = 0;
					total--;
					continue;
				}
			}

			// now see if this is a reference to a function primitive
			if (gate.equals("PMOStran"))
			{
				dumpFunction(gate, pMOStran, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("pMOStranWeak"))
			{
				dumpFunction(gate, pMOStranWeak, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("nMOStran"))
			{
				dumpFunction(gate, nMOStran, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("nMOStranWeak"))
			{
				dumpFunction(gate, nMOStranWeak, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("inverter"))
			{
				dumpFunction(gate, inverter, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("buffer"))
			{
				dumpFunction(gate, buffer, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.startsWith("and") && TextUtils.isDigit(gate.charAt(3)))
			{
				genFunction("and", true, false, TextUtils.atoi(gate.substring(3)), netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.startsWith("nand") && TextUtils.isDigit(gate.charAt(4)))
			{
				genFunction("and", true, true, TextUtils.atoi(gate.substring(4)), netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.startsWith("or") && TextUtils.isDigit(gate.charAt(2)))
			{
				genFunction("or", false, false, TextUtils.atoi(gate.substring(2)), netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.startsWith("nor") && TextUtils.isDigit(gate.charAt(3)))
			{
				genFunction("or", false, true, TextUtils.atoi(gate.substring(3)), netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("xor2"))
			{
				dumpFunction(gate, xor2, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("power"))
			{
				dumpFunction(gate, power, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("ground"))
			{
				dumpFunction(gate, ground, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("jkff"))
			{
				dumpFunction(gate, JKFF, netlist);
				uList.numRef = 0;
				total--;
			} else if (gate.equals("dsff"))
			{
				dumpFunction(gate, DFF, netlist);
				uList.numRef = 0;
				total--;
			}
		}

		// print unresolved reference list if not empty
		if (total > 0)
		{
			System.out.println("*****  UNRESOLVED REFERENCES *****");
			for (UnResList uList = unResolvedList; uList != null; uList = uList.next)
				if (uList.numRef > 0)
					System.out.println(uList.interfacef + ", " + uList.numRef + " time(s)");
		}
		return netlist;
	}

	/**
	 * Method to search library "lib" for a netlist that matches "name".  If found,
	 * add it to the current output netlist and return nonzero.  If not found, return false.
	 */
	private boolean addNetlist(Library lib, String name, List<String> netlist)
	{
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell np = (Cell)it.next();
			if (np.getView() != View.NETLISTALS) continue;
			StringBuffer infstr = new StringBuffer();
			String cellName = np.getName();
			for(int i=0; i<cellName.length(); i++)
			{
				char chr = cellName.charAt(i);
				if (TextUtils.isLetterOrDigit(chr)) infstr.append(chr); else
					infstr.append("_");
			}
			String key = infstr.toString();
			if (!name.equalsIgnoreCase(key)) continue;

			// add it to the netlist
			Variable var = np.getVar(Cell.CELL_TEXT_KEY);
			if (var == null) continue;
			String [] strings = (String [])var.getObject();
			netlist.add("");
			for(int i=0; i<strings.length; i++) netlist.add(strings[i]);
			return true;
		}
		return false;
	}

	/*
	 * inputs cap = 0.xx pF, Tphl = T1 + T2*Cin, Tplh = T3 + T4*Cin
	 */
	private void genFunction(String name, boolean isand, boolean isneg, int inputs, List<String> netlist)
	{
		// generate model name
		String modelName = "";
		if (isneg) modelName = "n";
		if (isand) modelName += "and"; else modelName += "or";
		modelName += inputs;

		netlist.add("");
		netlist.add("# Built-in model for " + modelName);

		// write header line
		StringBuffer infstr = new StringBuffer();
		infstr.append("model " + modelName + "(");
		for(int i=1; i<=inputs; i++)
		{
			if (i > 1) infstr.append(",");
			infstr.append("a" + i);
		}
		infstr.append(",z)");
		netlist.add(infstr.toString());

		// write function line
		infstr = new StringBuffer();
		infstr.append("g1: " + modelName + "fun(");
		for(int i=1; i<=inputs; i++)
		{
			if (i > 1) infstr.append(",");
			infstr.append("a" + i);
		}
		if (!isneg) infstr.append(",out)"); else
			infstr.append(",z)");
		netlist.add(infstr.toString());

		// write buffer line if not negated
		if (!isneg)
			netlist.add("g2: " + modelName + "buf(out,z)");

		// write function header
		infstr = new StringBuffer();
		infstr.append("gate " + modelName + "fun(");
		for(int i=1; i<=inputs; i++)
		{
			if (i > 1) infstr.append(",");
			infstr.append("a" + i);
		}
		infstr.append(",z)");
		netlist.add(infstr.toString());
		netlist.add("t: delta=1.33e-9");		/* T3=1.33 */
		for(int i=1; i<=inputs; i++)
		{
			infstr = new StringBuffer();
			infstr.append("i: a" + i);
			if (isand) infstr.append("=L o: z=H"); else
				infstr.append("=H o: z=L");
			netlist.add(infstr.toString());
		}
		netlist.add("t: delta=1.07e-9");		/* T1=1.07 */
		infstr = new StringBuffer();
		infstr.append("i:");
		for(int i=1; i<=inputs; i++)
		{
			if (isand) infstr.append(" a" + i + "=H"); else
				infstr.append(" a" + i + "=L");
		}
		if (isand) infstr.append(" o: z=L"); else
			infstr.append(" o: z=H");
		netlist.add(infstr.toString());
		netlist.add("t: delta=0");
		netlist.add("i: o: z=X");

		infstr = new StringBuffer();
		infstr.append("load:");
		for(int i=1; i<=inputs; i++)
		{
			infstr.append(" a" + i + "=1.0");
		}
		netlist.add(infstr.toString());

		// write buffer gate if not negated
		if (!isneg)
		{
			netlist.add("gate " + modelName + "buf(in,out)");
			netlist.add("t: delta=0.56e-9");		/* T4*Cin=0.56 */
			netlist.add("i: in=H    o: out=L");
			netlist.add("t: delta=0.41e-9");		/* T2*Cin=0.41 */
			netlist.add("i: in=L    o: out=H");
			netlist.add("t: delta=0");
			netlist.add("i: in=X    o: out=X");
		}
	}

	private void dumpFunction(String name, String [] model, List<String> netlist)
	{
		netlist.add("");
		netlist.add("# Built-in model for " + name);
		for(int i=0; i < model.length; i++) netlist.add(model[i]);
	}

	/**
	 * Method to recursively generate the ALS description for the specified model.
	 * Works by first generating the lowest interface instantiation and working back to the top (i.e. bottom up).
	 * @param interfacef pointer to interface.
	 * @param netlist the List of strings to create.
	 */
	private void genALSInterface(DBInterface interfacef, String name, List<String> netlist)
	{
		// go through interface's architectural body and call generate interfaces
		// for any interface called by an instance which has not been already generated

		// check written flag
		if ((interfacef.flags & ENTITY_WRITTEN) != 0) return;

		// set written flag
		interfacef.flags |= ENTITY_WRITTEN;

		// check all instants of corresponding architectural body
		// and write if non-primitive instances
		if (interfacef.bodies != null && interfacef.bodies.statements != null)
		{
			for (DBInstance inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
			{
				SymbolTree symbol = searchSymbol(inst.compo.name, globalSymbols);
				if (symbol == null)
				{
					if (EXTERNALENTITIES)
					{
						if (WARNFLAG)
							System.out.println("WARNING - interface " + inst.compo.name + " not found, assumed external.");
						addToUnresolved(inst.compo.name);
					} else
					{
						System.out.println("ERROR - interface " + inst.compo.name + " not found.");
					}
					continue;
				} else if (symbol.pointer == null)
				{
					// Should have gate entity
					// should be automatically added at end of .net file
				} else
				{
					genALSInterface((DBInterface)symbol.pointer, inst.compo.name, netlist);
				}
			}
		}

		// write this interface
		int generic = 0;
		boolean power_flag = false, ground_flag = false;
		StringBuffer infstr = new StringBuffer("model ");
		for(int i=0; i<name.length(); i++)
		{
			char chr = name.charAt(i);
			if (TextUtils.isLetterOrDigit(chr)) infstr.append(chr); else
				infstr.append('_');
		}
		infstr.append("(");

		// write port list of interface
		boolean first = true;
		for (DBPortList port = interfacef.ports; port != null; port = port.next)
		{
			if (port.type == null || port.type.type == DBTYPE_SINGLE)
			{
				if (!first) infstr.append(", ");
				infstr.append(port.name);
				first = false;
			} else
			{
				DBIndexRange iRange = (DBIndexRange)port.type.pointer;
				DBDiscreteRange dRange = iRange.dRange;
				if (dRange.start > dRange.end)
				{
					for (int i = dRange.start; i >= dRange.end; i--)
					{
						if (!first) infstr.append(", ");
						infstr.append(port.name + "[" + i + "]");
						first = false;
					}
				} else
				{
					for (int i = dRange.start; i <= dRange.end; i++)
					{
						if (!first) infstr.append(", ");
						infstr.append(port.name + "[" + i + "]");
						first = false;
					}
				}
			}
		}
		infstr.append(")");
		netlist.add(infstr.toString());

		// write all instances
		if (interfacef.bodies != null && interfacef.bodies.statements != null)
		{
			for (DBInstance inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
			{
				infstr = new StringBuffer();
				infstr.append(inst.name);
				infstr.append(": ");
				infstr.append(inst.compo.name);
				infstr.append("(");

				// print instance port list
				first = true;
				for (DBAPortList aPort = inst.ports; aPort != null; aPort = aPort.next)
				{
					if (aPort.name != null)
					{
						if (aPort.name.type == DBNAME_CONCATENATED)
						{
							// concatenated name
							for (DBNameList cat = (DBNameList)aPort.name.pointer; cat != null; cat = cat.next)
							{
								String ident = cat.name.name;
								if (ident.equalsIgnoreCase("power")) power_flag = true; else
									if (ident.equalsIgnoreCase("ground")) ground_flag = true;
								first = genAPort(infstr, first, cat.name);
							}
						} else
						{
							String ident = aPort.name.name;
							if (ident.equalsIgnoreCase("power")) power_flag = true; else
								if (ident.equalsIgnoreCase("ground")) ground_flag = true;
							first = genAPort(infstr, first, aPort.name);
						}
					} else
					{
						// check if formal port is of array type
						if (aPort.port.type != null && aPort.port.type.type == DBTYPE_ARRAY)
						{
							DBIndexRange iRange = (DBIndexRange)aPort.port.type.pointer;
							DBDiscreteRange dRange = iRange.dRange;
							if (dRange.start > dRange.end)
							{
								for (int i = dRange.start; i >= dRange.end; i--)
								{
									if (!first) infstr.append(", ");
									infstr.append("n" + (generic++));
									first = false;
								}
							} else
							{
								for (int i = dRange.start; i <= dRange.end; i++)
								{
									if (!first) infstr.append(", ");
									infstr.append("n" + (generic++));
									first = false;
								}
							}
						} else
						{
							if (!first) infstr.append(", ");
							infstr.append("n" + (generic++));
							first = false;
						}
					}
				}
				infstr.append(")");
				netlist.add(infstr.toString());
			}
		}

		// check for power and ground flags
		if (power_flag) netlist.add("set power = H@3");
			else if (ground_flag) netlist.add("set ground = L@3");
		netlist.add("");
	}

	/**
	 * Method to add the actual port for a single name to the infinite string.
	 */
	private boolean genAPort(StringBuffer infstr, boolean first, DBName name)
	{
		if (name.type == DBNAME_INDEXED)
		{
			if (!first) infstr.append(", ");
			infstr.append(name.name + ((DBExprList)(name.pointer)).value);
			first = false;
		} else
		{
			if (name.dbType != null && name.dbType.type == DBTYPE_ARRAY)
			{
				DBIndexRange iRange = (DBIndexRange)name.dbType.pointer;
				DBDiscreteRange dRange = iRange.dRange;
				if (dRange.start > dRange.end)
				{
					for (int i = dRange.start; i >= dRange.end; i--)
					{
						if (!first) infstr.append(", ");
						infstr.append(name.name + "[" + i + "]");
						first = false;
					}
				} else
				{
					for (int i = dRange.start; i <= dRange.end; i++)
					{
						if (!first) infstr.append(", ");
						infstr.append(name.name + "[" + i + "]");
						first = false;
					}
				}
			} else
			{
				if (!first) infstr.append(", ");
				infstr.append(name.name);
				first = false;
			}
		}
		return first;
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
		int		nameType;	/* type of name - simple or indexed */
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
		String	instName;	/* name of instance */
		String	portName;	/* name of port */
		QPORT	next;		/* next in port list */
	};

	/**
	 * Method to generate QUISC target output for the created parse tree.
	 * Assume parse tree is semantically correct.
     * @param destLib destination library.
	 * @return a list of strings that has the netlist.
	 */
	private List<String> genQuisc(Library destLib)
	{
		List<String> netlist = new ArrayList<String>();

		// print file header
		netlist.add("!*************************************************");
		netlist.add("!  QUISC Command file");
		netlist.add("!");
		if (User.isIncludeDateAndVersionInOutput())
			netlist.add("!  File Creation:    " + TextUtils.formatDate(new Date()));
		netlist.add("!-------------------------------------------------");
		netlist.add("");

		// determine top level cell
		DBInterface topInterface = findTopInterface(theUnits);
		if (topInterface == null)
			System.out.println("ERROR - Cannot find top interface."); else
		{
			// clear written flag on all entities
			for (DBInterface interfacef = theUnits.interfaces; interfacef != null;
				interfacef = interfacef.next) interfacef.flags &= ~ENTITY_WRITTEN;
			genQuiscInterface(topInterface, netlist);
		}

		// print closing line of output file
		netlist.add("!********* End of command file *******************");

		// scan unresolved references for reality inside of Electric
		Library cellLib = Library.findLibrary(SilComp.SCLIBNAME);
		int total = 0;
		for (UnResList uList = unResolvedList; uList != null; uList = uList.next)
		{
			// see if this is a reference to a cell in the destination library
			boolean found = false;
			for(Iterator<Cell> it = destLib.getCells(); it.hasNext(); )
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
				if (uList.interfacef.equalsIgnoreCase(sb.toString())) { found = true;   break; }
			}
			if (!found && cellLib != null)
			{
				for(Iterator<Cell> it = cellLib.getCells(); it.hasNext(); )
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
					if (uList.interfacef.equalsIgnoreCase(sb.toString())) { found = true;   break; }
				}
			}
			if (found)
			{
				uList.numRef = 0;
				continue;
			}
			total++;
		}

		// print unresolved reference list
		if (total > 0)
		{
			System.out.println("*****  UNRESOLVED REFERENCES *****");
			for (UnResList uList = unResolvedList; uList != null; uList = uList.next)
				if (uList.numRef > 0)
					System.out.println(uList.interfacef + ", " + uList.numRef + " time(s)");
		}
		return netlist;
	}

	/**
	 * Method to find the top interface in the database.
	 * The top interface is defined as the interface is called by no other architectural bodies.
	 * @param units pointer to database design units.
	 * @return pointer to top interface.
	 */
	private DBInterface findTopInterface(DBUnits units)
	{
		/* clear flags of all interfaces in database */
		for (DBInterface interfacef = units.interfaces; interfacef != null; interfacef = interfacef.next)
			interfacef.flags &= ~TOP_ENTITY_FLAG;

		/* go through the list of bodies and flag any interfaces */
		for (DBBody body = units.bodies; body != null; body = body.next)
		{
			/* go through component list */
			if (body.declare == null) continue;
			for (DBComponents compo = body.declare.components; compo != null; compo = compo.next)
			{
				SymbolTree symbol = searchSymbol(compo.name, globalSymbols);
				if (symbol != null && symbol.pointer != null)
				{
					((DBInterface)(symbol.pointer)).flags |= TOP_ENTITY_FLAG;
				}
			}
		}

		/* find interface with the flag bit not set */
		DBInterface interfacef;
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
	private void genQuiscInterface(DBInterface interfacef, List<String> netlist)
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
			for (DBInstance inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
			{
				SymbolTree symbol = searchSymbol(inst.compo.name, globalSymbols);
				if (symbol == null || symbol.pointer == null)
				{
					if (EXTERNALENTITIES)
					{
						if (WARNFLAG)
							System.out.println("WARNING - interface " + inst.compo.name + " not found, assumed external.");

						// add to unresolved list
						addToUnresolved(inst.compo.name);
					} else
						System.out.println("ERROR - interface " + inst.compo.name + "not found.");
					continue;
				} else genQuiscInterface((DBInterface)symbol.pointer, netlist);
			}
		}

		// write this entity
		netlist.add("create cell " + interfacef.name);

		// write out instances as components
		if (interfacef.bodies != null && interfacef.bodies.statements != null)
		{
			for (DBInstance inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
				netlist.add("create instance " + inst.name + " " + inst.compo.name);
		}

		// create export list
		QNODE qNodes = null;
		QNODE lastNode = null;
		for (DBPortList fPort = interfacef.ports; fPort != null; fPort = fPort.next)
		{
			if (fPort.type == null || fPort.type.type == DBTYPE_SINGLE)
			{
				QNODE newQNode = new QNODE();
				newQNode.name = fPort.name;
				newQNode.nameType = QNODE_SNAME;
				newQNode.size = 0;
				newQNode.start = 0;
				newQNode.end = 0;
				newQNode.table = null;
				newQNode.flags = QNODE_EXPORT;
				newQNode.mode = fPort.mode;
				newQNode.ports = null;
				newQNode.next = null;
				if (lastNode == null) qNodes = lastNode = newQNode; else
				{
					lastNode.next = newQNode;
					lastNode = newQNode;
				}
			} else
			{
				QNODE newQNode = new QNODE();
				newQNode.name = fPort.name;
				newQNode.nameType = QNODE_INAME;
				newQNode.flags = QNODE_EXPORT;
				newQNode.mode = fPort.mode;
				newQNode.ports = null;
				newQNode.next = null;
				if (lastNode == null) qNodes = lastNode = newQNode; else
				{
					lastNode.next = newQNode;
					lastNode = newQNode;
				}
				DBIndexRange iRange = (DBIndexRange)fPort.type.pointer;
				DBDiscreteRange dRange = iRange.dRange;
				newQNode.start = dRange.start;
				newQNode.end = dRange.end;
				int size = 1;
				if (dRange.start > dRange.end)
				{
					size = dRange.start - dRange.end + 1;
				} else if (dRange.start < dRange.end)
				{
					size = dRange.end - dRange.start + 1;
				}
				newQNode.size = size;
				newQNode.table = new QPORT[size];
				for (int i = 0; i < size; i++) newQNode.table[i] = null;
			}
		}

		// add local signals
		if (interfacef.bodies != null && interfacef.bodies.declare != null)
		{
			for (DBSignals signal = interfacef.bodies.declare.bodySignals; signal != null; signal = signal.next)
			{
				if (signal.type == null || signal.type.type == DBTYPE_SINGLE)
				{
					QNODE newQNode = new QNODE();
					newQNode.name = signal.name;
					newQNode.nameType = QNODE_SNAME;
					newQNode.size = 0;
					newQNode.start = 0;
					newQNode.end = 0;
					newQNode.table = null;
					if (signal.name.equalsIgnoreCase("power"))
					{
						newQNode.flags = QNODE_POWER;
					} else if (signal.name.equalsIgnoreCase("ground"))
					{
						newQNode.flags = QNODE_GROUND;
					} else
					{
						newQNode.flags = 0;
					}
					newQNode.mode = 0;
					newQNode.ports = null;
					newQNode.next = null;
					if (lastNode == null)
					{
						qNodes = lastNode = newQNode;
					} else
					{
						lastNode.next = newQNode;
						lastNode = newQNode;
					}
				} else
				{
					QNODE newQNode = new QNODE();
					newQNode.name = signal.name;
					newQNode.nameType = QNODE_INAME;
					newQNode.flags = 0;
					newQNode.mode = 0;
					newQNode.ports = null;
					newQNode.next = null;
					if (lastNode == null)
					{
						qNodes = lastNode = newQNode;
					} else
					{
						lastNode.next = newQNode;
						lastNode = newQNode;
					}
					DBIndexRange iRange = (DBIndexRange)signal.type.pointer;
					DBDiscreteRange dRange = iRange.dRange;
					newQNode.start = dRange.start;
					newQNode.end = dRange.end;
					int size = 1;
					if (dRange.start > dRange.end)
					{
						size = dRange.start - dRange.end + 1;
					} else if (dRange.start < dRange.end)
					{
						size = dRange.end - dRange.start + 1;
					}
					newQNode.size = size;
					newQNode.table = new QPORT[size];
					for (int i = 0; i < size; i++) newQNode.table[i] = null;
				}
			}
		}

		// write out connects
		if (interfacef.bodies != null && interfacef.bodies.statements != null)
		{
			for (DBInstance inst = interfacef.bodies.statements.instances; inst != null; inst = inst.next)
			{
				// check all instance ports for connections
				for (DBAPortList aPort = inst.ports; aPort != null; aPort = aPort.next)
				{
					if (aPort.name == null) continue;

					// get names of all members of actual port
					switch (aPort.name.type)
					{
						case DBNAME_IDENTIFIER:
							addIdentAPort(aPort.name, aPort.port, 0, inst, qNodes);
							break;
						case DBNAME_INDEXED:
							addIndexedAPort(aPort.name, aPort.port, 0, inst, qNodes);
							break;
						case DBNAME_CONCATENATED:
							int offset = 0;
							for (DBNameList cat = (DBNameList)aPort.name.pointer; cat != null; cat = cat.next)
							{
								if (cat.name.type == DBNAME_IDENTIFIER)
								{
									addIdentAPort(cat.name, aPort.port, offset, inst, qNodes);
								} else
								{
									addIndexedAPort(cat.name, aPort.port, offset, inst, qNodes);
								}
								offset += querySize(cat.name);
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
		for (QNODE newQNode = qNodes; newQNode != null; newQNode = newQNode.next)
		{
			if (newQNode.nameType == QNODE_SNAME)
			{
				QPORT qPort = newQNode.ports;
				if (qPort != null)
				{
					for (QPORT qPort2 = qPort.next; qPort2 != null; qPort2 = qPort2.next)
					{
						netlist.add("connect " + qPort.instName + " " + qPort.portName + " " + qPort2.instName + " " + qPort2.portName);
					}
					if ((newQNode.flags & QNODE_POWER) != 0)
					{
						netlist.add("connect " + qPort.instName + " " + qPort.portName + " power");
					}
					if ((newQNode.flags & QNODE_GROUND) != 0)
					{
						netlist.add("connect " + qPort.instName + " " + qPort.portName + " ground");
					}
				}
			} else
			{
				for (int i = 0; i < newQNode.size; i++)
				{
					QPORT qPort = newQNode.table[i];
					if (qPort != null)
					{
						for (QPORT qPort2 = qPort.next; qPort2 != null; qPort2 = qPort2.next)
						{
							netlist.add("connect " + qPort.instName + " " + qPort.portName + " " + qPort2.instName + " " + qPort2.portName);
						}
					}
				}
			}
		}

		// print out exports
		for (QNODE newQNode = qNodes; newQNode != null; newQNode = newQNode.next)
		{
			if ((newQNode.flags & QNODE_EXPORT) != 0)
			{
				if (newQNode.nameType == QNODE_SNAME)
				{
					QPORT qPort = newQNode.ports;
					if (qPort != null)
					{
						String inOut = "";
						switch (newQNode.mode)
						{
							case DBMODE_IN:  inOut = " input";    break;
							case DBMODE_OUT: inOut = " output";   break;
						}
						netlist.add("export " + qPort.instName + " " + qPort.portName + " " + newQNode.name + inOut);
					} else
					{
						System.out.println("ERROR - no export for " + newQNode.name);
					}
				} else
				{
					for (int i = 0; i < newQNode.size; i++)
					{
						int indexC = 0;
						if (newQNode.start > newQNode.end)
						{
							indexC = newQNode.start - i;
						} else
						{
							indexC = newQNode.start + i;
						}
						QPORT qPort = newQNode.table[i];
						if (qPort != null)
						{
							String inOut = "";
							switch (newQNode.mode)
							{
								case DBMODE_IN:  inOut = " input";    break;
								case DBMODE_OUT: inOut = " output";   break;
							}
							netlist.add("export " + qPort.instName + " " + qPort.portName + " " + newQNode.name + "[" + indexC + "]" + inOut);
						} else
						{
							System.out.println("ERROR - no export for " + newQNode.name + "[" + indexC + "]");
						}
					}
				}
			}
		}

		// extract entity
		netlist.add("extract");

		// print out non-exported node name assignments
		for (QNODE newQNode = qNodes; newQNode != null; newQNode = newQNode.next)
		{
			if ((newQNode.flags & QNODE_EXPORT) == 0)
			{
				if (newQNode.nameType == QNODE_SNAME)
				{
					QPORT qPort = newQNode.ports;
					if (qPort != null)
					{
						netlist.add("set node-name " + qPort.instName + " " + qPort.portName + " " + newQNode.name);
					}
				} else
				{
					for (int i = 0; i < newQNode.size; i++)
					{
						int indexC = 0;
						if (newQNode.start > newQNode.end)
						{
							indexC = newQNode.start - i;
						} else
						{
							indexC = newQNode.start + i;
						}
						QPORT qPort = newQNode.table[i];
						if (qPort != null)
						{
							netlist.add("set node-name " + qPort.instName + " " + qPort.portName + " " + newQNode.name + "[" + indexC + "]");
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
	private int querySize(DBName name)
	{
		int size = 0;
		if (name != null)
		{
			switch (name.type)
			{
				case DBNAME_IDENTIFIER:
					if (name.dbType != null)
					{
						switch (name.dbType.type)
						{
							case DBTYPE_SINGLE:
								size = 1;
								break;
							case DBTYPE_ARRAY:
								DBIndexRange iRange = (DBIndexRange)name.dbType.pointer;
								if (iRange != null)
								{
									DBDiscreteRange dRange = iRange.dRange;
									if (dRange != null)
									{
										if (dRange.start > dRange.end)
										{
											size = dRange.start - dRange.end;
										} else
										{
											size = dRange.end - dRange.start;
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
	 * @param qNodes address of start of node list.
	 */
	private void addIdentAPort(DBName name, DBPortList port, int offset, DBInstance inst, QNODE qNodes)
	{
		if (name.dbType != null && name.dbType.type == DBTYPE_ARRAY)
		{
			DBIndexRange iRange = (DBIndexRange)name.dbType.pointer;
			if (iRange != null)
			{
				DBDiscreteRange dRange = iRange.dRange;
				if (dRange != null)
				{
					int delta = 0;
					if (dRange.start > dRange.end)
					{
						delta = -1;
					} else if (dRange.start < dRange.end)
					{
						delta = 1;
					}
					int i = dRange.start - delta;
					int offset2 = 0;
					do
					{
						i += delta;
						QPORT newPort = createQPort(inst.name, port, offset + offset2);
						addQPortToQNode(newPort, name.name, QNODE_INAME, i, qNodes);
						offset2++;
					} while (i != dRange.end);
				}
			}
		} else
		{
			QPORT newPort = createQPort(inst.name, port, offset);
			addQPortToQNode(newPort, name.name, QNODE_SNAME, 0, qNodes);
		}
	}

	/**
	 * Method to add the actual port of indexed name type to the node list.
	 * @param name pointer to name.
	 * @param port pointer to port on component.
	 * @param offset offset in bits if of array type.
	 * @param inst pointer to instance of component.
	 * @param qNodes address of start of node list.
	 */
	private void addIndexedAPort(DBName name, DBPortList port, int offset, DBInstance inst, QNODE qNodes)
	{
		QPORT newPort = createQPort(inst.name, port, offset);
		int indexC = ((DBExprList)name.pointer).value;
		addQPortToQNode(newPort, name.name, QNODE_INAME, indexC, qNodes);
	}


	/**
	 * Method to create a qport for the indicated port.
	 * @param iname name of instance.
	 * @param port pointer to port on component.
	 * @param offset offset if array.
	 * @return address of created QPORT.
	 */
	private QPORT createQPort(String iname, DBPortList port, int offset)
	{
		QPORT newPort = new QPORT();
		newPort.instName = iname;
		newPort.next = null;
		if (port.type != null && port.type.type == DBTYPE_ARRAY)
		{
			newPort.portName = port.name + "[" + offset + "]";
		} else
		{
			newPort.portName = port.name;
		}

		return newPort;
	}

	/**
	 * Method to add the port to the node list.
	 * @param port port to add.
	 * @param ident name of node to add to.
	 * @param type if simple or indexed.
	 * @param indexC index if arrayed.
	 * @param qNodes address of pointer to start of list.
	 */
	private void addQPortToQNode(QPORT port, String ident, int type, int indexC, QNODE qNodes)
	{
		for (QNODE node = qNodes; node != null; node = node.next)
		{
			if (node.name.equalsIgnoreCase(ident))
			{
				if (node.nameType == type)
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
							tindex = node.start - indexC;
						} else
						{
							tindex = indexC - node.start;
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

	private void addToUnresolved(String name)
	{
		for (UnResList uList = unResolvedList; uList != null; uList = uList.next)
		{
			if (uList.interfacef.equals(name)) { uList.numRef++;   return; }
		}

		UnResList uList = new UnResList();
		uList.interfacef = name;
		uList.numRef = 1;
		uList.next = unResolvedList;
		unResolvedList = uList;
	}
}
