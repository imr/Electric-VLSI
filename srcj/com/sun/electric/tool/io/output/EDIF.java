/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EDIF.java
 * Input/output tool: EDIF netlist generator
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.io.IOTool;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This is the netlister for EDIF.
 */
public class EDIF extends Output
{
	private int io_edifo_gateindex;
	private boolean schematic_view;
	private double schematic_scale = 1.0;

	/**
	 * The main entry point for EDIF deck writing.
	 * @param cell the top-level cell to write.
	 * @param filePath the disk file to create with EDIF.
	 */
	public static void writeEDIFFile(Cell cell, VarContext context, String filePath)
	{
		EDIF out = new EDIF();
		if (out.openTextOutputStream(filePath)) return;

		out.io_writeediflibrary(cell);

		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the EDIF netlister.
	 */
	EDIF()
	{
	}
	
	/**
	 * Method to write a ".edif" file (or a ".foot" file for layouts)
	 * describing the current cell from the library "lib"
	 */
	private void io_writeediflibrary(Cell cell)
	{
		// initialize counters for automatic name generation
		io_edifo_gateindex = 1;
	
		// See if schematic view is requested
		schematic_view = IOTool.isEDIFUseSchematicView();
		double meters_to_lambda = TextUtils.convertDistance(1, Technology.getCurrent(), TextUtils.UnitScale.NONE);

		String name = io_ediftoken(cell.getName());
	
		// If this is a layout representation, then create the footprint
		if (cell.getView() == View.LAYOUT)
		{
			// default routing grid is 6.6u = 660 centimicrons
			int rgrid = 660;
//			var = getval((INTBIG) np, VNODEPROTO, VINTEGER, x_("EDIF_routegrid"));
//			if (var != NOVARIABLE) rgrid = var->addr;
			io_edifwritefootprint(cell, name, rgrid);
			return;
		}
		System.out.println("CANNOT WRITE EDIF SCHEMATICS YET");

		// write the header
		String header = "Electric VLSI Design System";
		if (User.isIncludeDateAndVersionInOutput())
		{
			header += ", version " + Version.getVersion();
		}
		EO_put_header(header, "EDIF Writer", cell.getLibrary().getName());
	
//		// write the external primitive reference library, if any
//		if (io_edifwritelibext(np))
//		{
//			// determine the primitives being used
//			for (tech = el_technologies; tech != NOTECHNOLOGY; tech = tech->nexttechnology)
//				for (lnp = tech->firstnodeproto; lnp != NONODEPROTO; lnp = lnp->nextnodeproto)
//					lnp->temp1 = 0;
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib->nextlibrary)
//				for (lnp = olib->firstnodeproto; lnp != NONODEPROTO; lnp = lnp->nextnodeproto)
//					lnp->temp1 = 0;
//	
//			// search recursively for all primitives used
//			if (io_edifsearch(np) != 0)
//			{
//				// advise user that generic primitives are being used
//				ttyputerr(_("WARNING: external primitive library undefined - using generic models"));
//	
//				// write out all primitives used in the library
//				EO_open_block(edif_stream, x_("library"));
//				EO_put_identifier(edif_stream, x_("lib0"));
//				EO_put_block(edif_stream, x_("edifLevel"), x_("0"));
//				EO_open_block(edif_stream, x_("technology"));
//				EO_open_block(edif_stream, x_("numberDefinition"));
//				if (schematic_view)
//				{
//					EO_open_block(edif_stream, x_("scale"));
//					EO_put_integer(edif_stream, io_edif_scale(lambda));
//					EO_put_float(edif_stream, meters_to_lambda);
//					EO_put_block(edif_stream, x_("unit"), x_("DISTANCE"));
//					EO_close_block(edif_stream, x_("scale"));
//				}
//				EO_close_block(edif_stream, x_("technology"));
//				for (tech = el_technologies; tech != NOTECHNOLOGY; tech = tech->nexttechnology)
//					for (lnp = tech->firstnodeproto; lnp != NONODEPROTO; lnp = lnp->nextnodeproto)
//				{
//					if (lnp->temp1 != 0)
//					{
//						// write primitive "lnp"
//						fun = (lnp->userbits & NFUNCTION) >> NFUNCTIONSH;
//						if (fun == NPUNKNOWN || fun == NPPIN || fun == NPCONTACT ||
//							fun == NPNODE || fun == NPCONNECT || fun == NPART) continue;
//						for (i = 0; i < lnp->temp1; i++)
//							io_edifwriteprim(lnp, i, fun);
//					}
//				}
//				EO_close_block(edif_stream, x_("library"));
//			}
//		}
//	
//		// search recursively for all external libraries required
//		backannotate = FALSE;
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib->nextlibrary)
//			for (lnp = olib->firstnodeproto; lnp != NONODEPROTO; lnp = lnp->nextnodeproto)
//		{
//			lnp->temp1 = 0;
//			lnp->temp2 = 0;
//		}
//		io_edifoextlibcount = 0;
//		io_edifextsearch(np);
//	
//		// mark all node prototypes for final netlisting
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib->nextlibrary)
//			for (lnp = olib->firstnodeproto; lnp != NONODEPROTO; lnp = lnp->nextnodeproto)
//				lnp->temp1 = 0;
//	
//		// write out all external references in the library
//		if (io_edifoextlibcount > 0)
//			for (i = 1; i <= io_edifoextlibcount; i++)
//		{
//			EO_open_block(edif_stream, x_("external"));
//			(void)esnprintf(name, 100, x_("schem_lib_%ld"), i);
//			EO_put_identifier(edif_stream, name);
//			EO_put_block(edif_stream, x_("edifLevel"), x_("0"));
//			EO_open_block(edif_stream, x_("technology"));
//			EO_open_block(edif_stream, x_("numberDefinition"));
//			if (schematic_view)
//			{
//				EO_open_block(edif_stream, x_("scale"));
//				EO_put_integer(edif_stream, io_edif_scale(lambda));
//				EO_put_float(edif_stream, meters_to_lambda);
//				EO_put_block(edif_stream, x_("unit"), x_("DISTANCE"));
//				EO_close_block(edif_stream, x_("scale"));
//			}
//			EO_close_block(edif_stream, x_("technology"));
//			if (io_edifwritecell(np, i)) backannotate = TRUE;
//			EO_close_block(edif_stream, x_("external"));
//		}
//	
//		// now recursively write the cells expanded within the library
//		EO_open_block(edif_stream, x_("library"));
//		EO_put_identifier(edif_stream, io_ediftoken(lib->libname));
//		EO_put_block(edif_stream, x_("edifLevel"), x_("0"));
//		EO_open_block(edif_stream, x_("technology"));
//		EO_open_block(edif_stream, x_("numberDefinition"));
//		if (schematic_view)
//		{
//			EO_open_block(edif_stream, x_("scale"));
//			EO_put_integer(edif_stream, io_edif_scale(lambda));
//			EO_put_float(edif_stream, meters_to_lambda);
//			EO_put_block(edif_stream, x_("unit"), x_("DISTANCE"));
//			EO_close_block(edif_stream, x_("scale"));
//		}
//		EO_close_block(edif_stream, x_("technology"));
//	
//		if (io_edifwritecell(np, 0)) backannotate = TRUE;
//		EO_close_block(edif_stream, x_("library"));
//	
//		// post-identify the design and library
//		EO_open_block(edif_stream, x_("design"));
//		EO_put_identifier(edif_stream, io_ediftoken(np->protoname));
//		EO_open_block(edif_stream, x_("cellRef"));
//		EO_put_identifier(edif_stream, io_ediftoken(np->protoname));
//		EO_put_block(edif_stream, x_("libraryRef"), io_ediftoken(lib->libname));
//	
//		// clean up
//		ttyputmsg(_("%s written"), edif_stream->filename);
//		EO_close_stream(edif_stream);
//		if (backannotate)
//			ttyputmsg(_("Back-annotation information has been added (library must be saved)"));
//		return(FALSE);
	}
	
	private void EO_put_header(String program, String comment, String origin)
	{
//		CHAR name[WORD+1];
//		NODEPROTO *np;
//	
//		// output the standard EDIF 2 0 0 header
//		if (EO_open_block(stream, x_("edif"))) return(1);
//		np = el_curlib->curnodeproto;
//		(void)esnprintf(name, WORD+1, x_("%s"), np->protoname);
//		if (EO_put_identifier(stream, name)) return(1);
//		if (EO_put_block(stream, x_("edifVersion"), x_("2 0 0"))) return(1);
//		if (EO_put_block(stream, x_("edifLevel"), x_("0"))) return(1);
//		if (EO_open_block(stream, x_("keywordMap"))) return(1);
//		if (EO_put_block(stream, x_("keywordLevel"), x_("0"))) return(1);		// was "1"
//		if (EO_close_block(stream, x_("keywordMap"))) return(1);
//		if (EO_open_block(stream, x_("status"))) return(1);
//		if (EO_open_block(stream, x_("written"))) return(1);
//		if (EO_put_block(stream, x_("timeStamp"), EO_get_timestamp())) return(1);
//		if (program != NULL && EO_put_block(stream, x_("program"), EO_make_string(program))) return(1);
//		if (comment != NULL && EO_put_block(stream, x_("comment"), EO_make_string(comment))) return(1);
//		if (origin != NULL && EO_put_block(stream, x_("dataOrigin"), EO_make_string(origin))) return(1);
//		if (EO_close_block(stream, x_("status"))) return(1);
//		return(0);
	}

	/*
	 * Write the EDIF format of the footprint, assuming standard
	 * cell grid of routegrid (centimicrons) and standard connections
	 * only on Metal1 and Metal2.  cellType is obtained from the
	 * stopExpand property, which is a property attached to
	 * either the layout view or the schematic view (if there is one).
	 */
	private void io_edifwritefootprint(Cell cell, String name, int routegrid)
	{
//		String iname = name + ".foot";
//		io_fileout = xcreate(iname, io_filetypeedif, x_("EDIF File"), &truename);
//		if (io_fileout == NULL)
//		{
//			if (truename != 0) ttyputerr(_("Cannot write %s"), iname);
//			return(TRUE);
//		}
	
		// calculate the actual routing grid in microns
		double route = routegrid / 100;
	
		// write the header
		System.out.println("Writing footprint for cell " + io_ediftoken(cell.getName()));
		printWriter.println("(footprint " + TextUtils.formatDouble(route) + "e-06");
	
		// identify the layout type from the schematic 'stopExpand' property
		String cellType = "unknownLayoutRep";
		printWriter.println(" (" + cellType);
	
		// get representation type from schematic (or layout) 'repType' property
		String cellRep = "standard";
	
		// get standard cell dimensions
		Rectangle2D cellBounds = cell.getBounds();
		double width = cellBounds.getWidth() / routegrid;
		double height = cellBounds.getHeight() / routegrid;
		printWriter.println("  (" + io_ediftoken(cell.getName()) + " " + cellRep + " (" +
			TextUtils.formatDouble(height) + " " + TextUtils.formatDouble(width) + ")");
	
		// find out if all port connections are wanted
		boolean allports = false;
	
		// find all ports
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export e = (Export)it.next();

			// global connections are made in standard cells by abutment
			if (!allports && e.isBodyOnly()) continue;

			// find the connecting layers
			String portAccess = "";
			io_edifwriteportpositions(e, cellBounds.getMinX(), cellBounds.getMinY(), portAccess, routegrid);
		}
	
		// finish up
		printWriter.println(")))");
	}
	
	/*
	 * Access rules for port positions are in the port variable "EDIF_access"
	 * which takes the form "material:DIR,material:DIR," for all valid
	 * materials.
	 */
	private void io_edifwriteportpositions(Export pp, double cellx, double celly,
		String accessrules, int routegrid)
	{
		// establish the subnodeinst on which this port prototype resides
		NodeInst ni = pp.getOriginalPort().getNodeInst();
	
		// find which port proto is exported here
		PortProto ppt = pp.getOriginalPort().getPortProto();
	
		// get the port signal direction
		String portdirection = "unknown";
		if (pp.getName().equalsIgnoreCase("feed")) portdirection = "feed";
		if (pp.getCharacteristic() == PortProto.Characteristic.IN ||
			pp.getCharacteristic() == PortProto.Characteristic.REFIN) portdirection = "input"; else
		if (pp.getCharacteristic() == PortProto.Characteristic.OUT ||
			pp.getCharacteristic() == PortProto.Characteristic.REFOUT) portdirection = "output"; else
		if (pp.getCharacteristic() == PortProto.Characteristic.BIDIR) portdirection = "inout";
	
		// get this port geometry
		Poly portPoly = pp.getOriginalPort().getPoly();
		double cX = portPoly.getCenterX();
		double cY = portPoly.getCenterY();
	
		// offset center based on cell lower left corner
		cX -= cellx;
		cY -= celly;
	
		// default all connection sizes to 4x4 microns
		double xsize = 4, ysize = 4;
	
		// make floating point port positions and scale them
		double scale = routegrid;
		double xpos = cX / scale;
		double ypos = cY / scale;
	
//		i = j = 0;
//		connections = FALSE;
//		for (pt = accessrules;; pt++)
//		{
//			if (i == 0)
//			{
//				if (*pt == 0) break;
//				if (*pt != ':')
//				{
//					material[j] = *pt;
//					if (isupper(material[j])) tolower(material[j]);
//					j++;
//					continue;
//				}
//				material[j] = 0;
//				i++;
//				j = 0;
//				continue;
//			}
//			if (*pt != ',' && *pt != 0)
//			{
//				accessdirection[j++] = *pt;
//				continue;
//			}
//			accessdirection[j] = 0;
//			xprintf(io_fileout, x_("   (%-9s %-7s (%-7s (%5.2f %5.2f) (%lde-6 %lde-6) %s))\n"),
//				(namesame(portdirection, x_("feed")) == 0 ? io_edifupperstring(io_ediftoken(pp->protoname)) :
//					io_ediflowerstring(io_ediftoken(pp->protoname))), portdirection, material,
//						xpos, ypos, xsize, ysize, accessdirection);
//			connections = TRUE;
//	
//			if (*pt == 0) break;
//			i = j = 0;
//		}
//		return(connections);
	}

	/*
	 * convert a string token into a valid EDIF string token (note - NOT re-entrant coding)
	 * In order to use NSC program ce2verilog, we need to suppress the '_' which replaces
	 * ']' in bus definitions.
	 */
	private String io_ediftoken(String str)
	{
		if (str.length() == 0) return str;
		StringBuffer sb = new StringBuffer();
		if (Character.isDigit(str.charAt(0))) sb.append('X');
		for(int i=0; i<str.length(); i++)
		{
			char chr = str.charAt(i);
			if (Character.isWhitespace(chr)) break;
			if (chr == '[') chr = '_';
			if (Character.isLetterOrDigit(chr) || chr == '&' || chr == '_')
				sb.append(chr);
		}
		return sb.toString();
	}
}

//	#define WORD	   256
//	#define MAXDEPTH	40
//	#define LINE      1024
//	
//	#define GETBLANKS(C, M) (C ? x_("") : &io_edifo_blanks[sizeof(io_edifo_blanks)-(M)-1])
//	#define DOSPACE         (stream->compress ? x_("") : x_(" "))
//	#define NOEO_STREAM     ((struct _eo_stream *)0)
//	
//	// primary stream structure
//	typedef enum { EO_OPENED, EO_CLOSED, EO_TCLOSED } EO_FSTATE;
//	typedef struct _eo_stream
//	{
//		CHAR              *filename;			// the file name
//		FILE              *file;				// the opened stream
//		EO_FSTATE          state;				// the file state
//		INTBIG             fpos;				// the saved file position
//		CHAR              *blkstack[MAXDEPTH];	// the stream keyword stack
//		INTBIG             blkstack_ptr;		// the current position
//		struct _eo_stream *next;				// the next stream file
//		INTBIG             compress;			// the compress flag
//	} EO_STREAM, *EO_STREAM_PTR;
//	
//	static CHAR  *io_edifoextlibname[WORD+1];
//	static INTBIG io_edifoextlibcount;
//	static CHAR   io_edifo_blanks[] = {x_("                                                  ")};
//	
//	// globals for SCHEMATIC View
//	typedef enum
//	{
//		EGUNKNOWN = 0,
//		EGART = 1,
//		EGTEXT = 2,
//		EGWIRE = 3,
//		EGBUS = 4
//	} _egraphic;
//	static _egraphic egraphic = EGUNKNOWN;
//	static _egraphic egraphic_override = EGUNKNOWN;
//	static CHAR *egraphic_text[5] = {x_("UNKNOWN"), x_("ARTWORK"), x_("TEXT"), x_("WIRE"), x_("BUS")};
//	static EO_STREAM_PTR edif_stream;
//	
//	/*
//	 * routine to count the usage of primitives hierarchically below cell "np"
//	 */
//	INTBIG io_edifsearch(NODEPROTO *np)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *onp;
//		REGISTER PORTARCINST *pi;
//		REGISTER INTBIG i, fun, primcount;
//	
//		// do not search this cell if it is an icon
//		if (np->cellview == el_iconview) return(0);
//	
//		// keep a count of the total number of primitives encountered
//		primcount = 0;
//	
//		for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex != 0)
//			{
//				fun = nodefunction(ni);
//				i = 1;
//				if (fun == NPGATEAND || fun == NPGATEOR || fun == NPGATEXOR)
//				{
//					// count the number of inputs
//					for (pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//						if (pi->proto == ni->proto->firstportproto) i++;
//				}
//				ni->proto->temp1 = maxi(ni->proto->temp1, i);
//	
//				if (fun != NPUNKNOWN && fun != NPPIN && fun != NPCONTACT &&
//					fun != NPNODE && fun != NPCONNECT && fun != NPMETER &&
//						fun != NPCONPOWER && fun != NPCONGROUND && fun != NPSOURCE &&
//							fun != NPSUBSTRATE && fun != NPWELL && fun != NPART)
//								primcount++;
//				continue;
//			}
//	
//			// ignore recursive references (showing icon in contents)
//			if (isiconof(ni->proto, np)) continue;
//	
//			// get actual subcell (including contents/body distinction)
//			onp = contentsview(ni->proto);
//			if (onp == NONODEPROTO) onp = ni->proto;
//	
//			// search the subcell
//			if (onp->temp1 == 0) primcount += io_edifsearch(onp);
//		}
//		np->temp1++;
//		return(primcount);
//	}
//	
//	/*
//	 * Routine to identify cells with external connection models hierarchically below
//	 * cell "np" and locate the libraries that contain their models.  Sets np->temp2
//	 * to point to the location in the external library array containing the library
//	 * name.
//	 */
//	void io_edifextsearch(NODEPROTO *np)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *onp, *subnp;
//		REGISTER INTBIG i;
//		CHAR *libname;
//		REGISTER void *infstr;
//	
//		// return if primitive (handled separately)
//		if (np->primindex != 0) return;
//	
//		// check if this is a previously unencountered icon with no contents
//		onp = contentsview(np);
//		if (np->temp2 == 0 && np->cellview == el_iconview && onp == NONODEPROTO)
//		{
//			// do not mention monitor_probes
//			if (namesame(np->protoname, x_("monitor_probe")) == 0) return;
//	
//			// this cell is not expanded in this library
//			libname = io_ediffind_path(np);
//	
//			if (io_edifoextlibcount == 0) i = 1; else
//				for (i = 1; i <= io_edifoextlibcount; i++)
//					if (namesame(libname, io_edifoextlibname[i]) == 0) break;
//			if (i > io_edifoextlibcount)
//			{
//				io_edifoextlibcount = i;
//				infstr = initinfstr();
//				addstringtoinfstr(infstr, libname);
//				(void)allocstring(&io_edifoextlibname[i], returninfstr(infstr), io_tool->cluster);
//				ttyputmsg(_("External library %s, lib number %ld"), io_edifoextlibname[i], i);
//			}
//			np->temp2 = i;
//			np->temp1++;
//			return;
//		}
//		if (onp == NONODEPROTO) onp = np;
//		for (ni = onp->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			subnp = ni->proto;
//			if (subnp->temp1 != 0) continue;
//	
//			// ignore recursive references (showing icon in contents)
//			if (isiconof(subnp, onp)) continue;
//			io_edifextsearch(subnp);
//		}
//	
//		np->temp1++;
//	}
//	
//	/*
//	 * Routine to find the path to the EDIF file in the standard library
//	 * structure, as contained in /usr/local/electric/lib/edifpath.
//	 */
//	CHAR *io_ediffind_path(NODEPROTO *np)
//	{
//		CHAR *path, libpath[255], ch, *filename;
//		FILE *f, *etry;
//		INTBIG i;
//	
//		// initialize the local path name variable
//		f = xopen(x_("edifpath"), io_filetypeedif, el_libdir, &filename);
//		if (f != NULL)
//		{
//			for(ch = (CHAR)xgetc(f), i = 0; ; ch = (CHAR)xgetc(f), i++)
//			{
//				if (ch == ' ' || ch == '\t') continue;
//				if (ch == '\n' || ch == EOF || ch == ';')
//				{
//					libpath[i] = DIRSEP;
//					libpath[i + 1] = 0;
//					estrcat(libpath, np->protoname);
//					estrcat(libpath, DIRSEPSTR);
//					estrcat(libpath, x_("edif"));
//					estrcat(libpath, DIRSEPSTR);
//					estrcat(libpath, x_("source"));
//					etry = xopen(libpath, io_filetypeedif, el_libdir, &filename);
//					if (etry != NULL)
//					{
//						libpath[i] = 0;
//						xclose(etry);
//						break;
//					}
//					i = -1;			// start another line after incrementing i
//				} else libpath[i] = ch;
//				if (ch == EOF)
//				{
//					(void)estrcpy(libpath, x_("unknown"));
//					break;
//				}
//			}
//			xclose(f);
//		} else (void)estrcpy(libpath, x_("unknown"));
//	
//		(void)allocstring(&path, libpath, io_tool->cluster);
//		return(path);
//	}
//	
//	/*
//	 * routine to dump the description of primitive "np" to the EDIF file
//	 * If the primitive is a schematic gate, use "i" as the number of inputs
//	 */
//	void io_edifwriteprim(NODEPROTO *np, INTBIG i, INTBIG fun)
//	{
//		REGISTER INTBIG j;
//		REGISTER PORTPROTO *firstport, *pp;
//		REGISTER CHAR *direction;
//		CHAR name[100];
//	
//		// write primitive name
//		if (fun == NPGATEAND || fun == NPGATEOR || fun == NPGATEXOR)
//		{
//			EO_open_block(edif_stream, x_("cell"));
//			(void)esnprintf(name, 100, x_("%s%ld"), io_ediftoken(np->protoname), i);
//			EO_put_identifier(edif_stream, name);
//		} else
//		{
//			EO_open_block(edif_stream, x_("cell"));
//			EO_put_identifier(edif_stream, io_ediftoken(np->protoname));
//		}
//	
//		// write primitive connections
//		EO_put_block(edif_stream, x_("cellType"), x_("GENERIC"));
//		EO_open_block(edif_stream, x_("view"));
//		EO_put_identifier(edif_stream, x_("cell"));
//		EO_put_block(edif_stream, x_("viewType"), (CHAR *)(schematic_view ? x_("SCHEMATIC") : x_("NETLIST")));
//		EO_open_block(edif_stream, x_("interface"));
//	
//		firstport = np->firstportproto;
//		if (fun == NPGATEAND || fun == NPGATEOR || fun == NPGATEXOR)
//		{
//			for (j = 0; j < i; j++)
//			{
//				EO_open_block(edif_stream, x_("port"));
//				(void)esnprintf(name, 100, x_("IN%ld"), j + 1);
//				EO_put_identifier(edif_stream, name);
//				EO_put_block(edif_stream, x_("direction"), x_("INPUT"));
//				EO_close_block(edif_stream, x_("port"));
//			}
//			firstport = np->firstportproto->nextportproto;
//		}
//		for (pp = firstport; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			switch (pp->userbits & STATEBITS)
//			{
//				case OUTPORT:
//					direction = x_("output");
//					break;
//				case BIDIRPORT:
//					direction = x_("inout");
//					break;
//				default:
//					direction = x_("input");
//					break;
//			}
//			EO_open_block(edif_stream, x_("port"));
//			EO_put_identifier(edif_stream, io_ediftoken(pp->protoname));
//			EO_put_block(edif_stream, x_("direction"), direction);
//			EO_close_block(edif_stream, x_("port"));
//		}
//		if (schematic_view)
//		{
//			// EMPTY 
//		}
//		EO_close_block(edif_stream, x_("cell"));
//	}
//	
//	/* module: io_edif_scale
//	 * function: will scale the requested integer
//	 * returns the scaled value
//	 */
//	INTBIG io_edif_scale(INTBIG val)
//	{
//		if (val < 0)
//			return((INTBIG)(((double) val - 0.5) * schematic_scale));
//		return((INTBIG)(((double) val + 0.5) * schematic_scale));
//	}
//	
//	/*
//	 * Routine to map Electric orientations to EDIF orientations
//	 */
//	CHAR *io_edif_orientation(NODEINST *ni)
//	{
//		if (ni->transpose)
//		{
//			switch (ni->rotation)
//			{
//				case 0:    return(x_("MYR90"));
//				case 900:  return(x_("MY"));
//				case 1800: return(x_("MXR90"));
//				case 2700: return(x_("MX"));
//			}
//		} else
//		{
//			switch (ni->rotation)
//			{
//				case 0:    return(x_("R0"));
//				case 900:  return(x_("R90"));
//				case 1800: return(x_("R180"));
//				case 2700: return(x_("R270"));
//			}
//		}
//		return(x_("ERROR"));
//	}
//	
//	/*
//	 * Helper name builder
//	 */
//	CHAR *io_edifwritecompname(NODEINST *ni, INTBIG fun, INTBIG serindex)
//	{
//		REGISTER VARIABLE *var;
//		REGISTER CHAR *okname;
//		static CHAR name[WORD+1];
//		static INTBIG EDIF_name_key = 0;
//		Q_UNUSED( fun );
//		Q_UNUSED( serindex );
//	
//		if (EDIF_name_key == 0) EDIF_name_key = makekey(x_("EDIF_name"));
//	
//		// always use EDIF_name if required
//		var = getvalkey((INTBIG) ni, VNODEINST, VSTRING, EDIF_name_key);
//		okname = io_edifvalidname(var);
//		if (okname == 0)
//		{
//			// check for node name
//			var = getvalkey((INTBIG) ni, VNODEINST, VSTRING, el_node_name_key);
//			okname = io_edifvalidname(var);
//			if (okname == 0)
//			{
//				// create a new EDIF_name
//				(void)esnprintf(name, WORD+1, x_("INSTANCE%ld"), io_edifo_gateindex++);
//				(void)setvalkey((INTBIG) ni, VNODEINST, EDIF_name_key, (INTBIG)name, VSTRING);
//			} else
//			{
//				(void)setvalkey((INTBIG) ni, VNODEINST, EDIF_name_key, (INTBIG)okname, VSTRING);
//			}
//		}
//	
//		if (isdigit(*okname) || *okname == '_')
//			(void)esnprintf(name, WORD+1, x_("&%s"), okname); else
//				(void)estrcpy(name, okname);
//		return(name);
//	}
//	
//	/* module: io_edif_pt
//	 * function: will generate a pt symbol (pt x y)
//	 * returns success or failure
//	 */
//	void io_edif_pt(INTBIG x, INTBIG y)
//	{
//		EO_open_block(edif_stream, x_("pt"));
//		EO_put_integer(edif_stream, io_edif_scale(x));
//		EO_put_integer(edif_stream, io_edif_scale(y));
//		EO_close_block(edif_stream, x_("pt"));
//	}
//	
//	/*
//	 * Routine to recursively dump cell "np" to the EDIF file
//	 * Recurses for contents if 'external' is zero, creates only
//	 * interface models for external elements for library number
//	 * 'external' if 'external' is non-zero.
//	 * Returns true if back-annotation was added.
//	 */
//	BOOLEAN io_edifwritecell(NODEPROTO *np, INTBIG external)
//	{
//		INTBIG i, netcount, displaytotal;
//		INTBIG fun;
//		BOOLEAN backannotate;
//		NETWORK *net, *cnet, *onet;
//		VARIABLE *var;
//		NODEINST *ni;
//		PORTARCINST *pi;
//		PORTEXPINST *pe;
//		PORTPROTO *pp, *cpp;
//		NODEPROTO *onp, *cnp, *lnp;
//		ARCINST *ai;
//		CHAR *pt, *iname, *oname, line[WORD+1], *direction;
//		BOOLEAN globalport;
//		BOOLEAN contents, schematic;
//		INTBIG netindex, pageindex, is_array, sx, mx, rx, sy, my, ry;
//		INTBIG diffcount;
//		XARRAY trans;
//		INTBIG bx, by, xpos, ypos;
//		CHAR name[WORD+1], page[10];
//		static POLYGON *poly = NOPOLYGON;
//		static INTBIG EDIF_name_key = 0;
//		static INTBIG EDIF_array_key = 0;
//	
//		if (EDIF_name_key == 0) EDIF_name_key = makekey(x_("EDIF_name"));
//		if (EDIF_array_key == 0) EDIF_array_key = makekey(x_("EDIF_array"));
//	
//		// stop if requested
//		if (el_pleasestop != 0)
//		{
//			stopping(STOPREASONEDIF);
//			return(FALSE);
//		}
//	
//		// get polygon
//		if (schematic_view)
//			(void)needstaticpolygon(&poly, 4, io_tool->cluster);
//	
//		// recurse on sub-cells first
//		backannotate = FALSE;
//		if (np->cellview != el_iconview)
//		{
//			for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			{
//				ni->temp1 = 0;
//				if (ni->proto->primindex != 0) continue;
//	
//				// ignore recursive references (showing icon in contents)
//				if (isiconof(ni->proto, np)) continue;
//	
//				// do not expand "monitor_probe" construct (icon)
//				if (namesame(ni->proto->protoname, x_("monitor_probe")) == 0) continue;
//	
//				// get actual subcell (including contents/body distinction)
//				onp = contentsview(ni->proto);
//				if (onp == NONODEPROTO) onp = ni->proto;
//	
//				// write the subcell
//				if (onp->temp1 == 0)
//				{
//					if (io_edifwritecell(onp, external) != 0) backannotate = TRUE;
//				}
//			}
//		}
//	
//		// check whether writing external or contents cells
//		if (external > 0 && np->cellview != el_iconview) return(backannotate);
//	
//		// check whether this cell is in this external library
//		if (external > 0 && np->temp2 != external) return(backannotate);
//	
//		// make sure that all nodes and networks have names on them
//		if (asktool(net_tool, x_("name-nodes"), (INTBIG)np) != 0) backannotate++;
//		if (asktool(net_tool, x_("name-nets"), (INTBIG)np) != 0) backannotate++;
//	
//		// mark this cell as written
//		np->temp1++;
//	
//		// assign bus names to unnamed bus wires connecting bus ports on objects
//		netindex = 1;
//		// sim_namebusnets(np, &netindex);
//	
//		// clear unnamed net identifier field
//		for (net = np->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//			net->temp1 = 0;
//	
//		// write out the cell header information
//		EO_open_block(edif_stream, x_("cell"));
//		EO_put_identifier(edif_stream, io_ediftoken(np->protoname));
//		EO_put_block(edif_stream, x_("cellType"), x_("generic"));
//		EO_open_block(edif_stream, x_("view"));
//		EO_put_identifier(edif_stream, x_("cell"));
//		EO_put_block(edif_stream, x_("viewType"), (CHAR *)(schematic_view ? x_("SCHEMATIC") : x_("NETLIST")));
//	
//		// write out the interface description
//		EO_open_block(edif_stream, x_("interface"));
//	
//		// list all ports in interface except global ports in network order
//		(void)io_edifmarknetports(np);
//	
//		// count check on differentialGroup property
//		diffcount = 0;
//		for (net = np->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//		{
//			if ((pp = (PORTPROTO *) net->temp2) != NOPORTPROTO && !io_edifisglobal(pp))
//			{
//				switch (pp->userbits & STATEBITS)
//				{
//					case OUTPORT:
//						direction = x_("OUTPUT");
//						break;
//					case BIDIRPORT:
//						direction = x_("INOUT");
//						break;
//					case REFOUTPORT:
//						direction = x_("OUTPUT");
//						break;
//					case INPORT:
//					case REFINPORT:
//					default:
//						direction = x_("INPUT");
//						break;
//				}
//				EO_open_block(edif_stream, x_("port"));
//				EO_put_identifier(edif_stream, io_ediftoken(networkname(net, 0)));
//				if (estrlen(direction) > 0)
//					EO_put_block(edif_stream, x_("direction"), direction);
//	
//				// list port properties if they exist on this schematic
//				// if (np->cellview != el_iconview) listPortProperties(pp, direction, net, diffcount);  
//	
//				EO_close_block(edif_stream, x_("port"));
//			}
//		}
//		if (schematic_view && np->cellview == el_iconview)
//		{
//			// output the icon
//			io_edifsymbol(np);
//		}
//	
//		EO_close_block(edif_stream, x_("interface"));
//	
//		if (diffcount != 0)
//			ttyputmsg(_("** WARNING - unmatched constructed differentialGroup property in %s"),
//				describenodeproto(np));
//	
//		// list contents if expanding
//		if (np->cellview != el_iconview)
//		{
//			// write the components, if there are any
//			contents = FALSE;
//	
//			// determine if this is a schematic view
//			if (schematic_view && !estrncmp(np->cellview->viewname, x_("schematic-page-"), 15))
//			{
//				// set beginning page
//				pageindex = 1;
//				(void)esnprintf(page, 10, x_("P%ld"), pageindex);
//	
//				// locate the next like in cell
//				FOR_CELLGROUP(lnp, np)
//					if (!namesame(lnp->cellview->sviewname, page))
//				{
//					// list all ports in interface except global ports in network order
//					(void)io_edifmarknetports(lnp);
//					break;
//				}
//				schematic = TRUE;
//			} else schematic = FALSE;
//	
//			contents = FALSE;
//			while (np != NONODEPROTO)
//			{
//				if (np->firstnodeinst != NONODEINST)
//				{
//					contents = TRUE;
//					if (!contents) EO_open_block(edif_stream, x_("contents"));
//					if (schematic_view && schematic)
//					{
//						EO_open_block(edif_stream, x_("page"));
//						EO_put_identifier(edif_stream, np->cellview->sviewname);
//					}
//					for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					{
//						fun = nodefunction(ni);
//						if (ni->proto->primindex != 0)
//						{
//							if (fun == NPUNKNOWN || fun == NPPIN || fun == NPCONTACT ||
//								fun == NPNODE || fun == NPCONNECT || fun == NPART) continue;
//						} else if (namesame(ni->proto->protoname, x_("monitor_probe")) == 0)
//							continue;
//	
//						// ignore recursive references (showing icon in contents)
//						if (isiconof(ni->proto, np)) continue;
//	
//						EO_open_block(edif_stream, x_("instance"));
//						iname = io_edifwritecompname(ni, fun, 0);
//						if ((var = getvalkey((INTBIG) ni, VNODEINST, VSTRING, el_node_name_key)) != NOVARIABLE)
//							oname = (CHAR *)var->addr; else
//								oname = iname;
//	
//						// check for an array
//						is_array = 0;
//						if ((var = getvalkey((INTBIG) ni, VNODEINST, VSTRING, EDIF_array_key)) != NOVARIABLE)
//						{
//							// decode the array bounds min:max:range min:max:range
//							(void)esscanf((CHAR *)var->addr, x_("%ld:%ld:%ld %ld:%ld:%ld"), &sx, &mx, &rx, &sy, &my, &ry);
//							if (sx != mx || sy != my)
//							{
//								is_array = 1;
//								EO_open_block(edif_stream, x_("array"));
//							}
//						}
//	
//						if (namesame(oname, iname))
//						{
//							EO_open_block(edif_stream, x_("rename"));
//							EO_put_identifier(edif_stream, iname);
//							EO_put_string(edif_stream, oname);
//							EO_close_block(edif_stream, x_("rename"));
//						} else EO_put_identifier(edif_stream, iname);
//	
//						if (is_array)
//						{
//							if (rx > 1) EO_put_integer(edif_stream, rx);
//							if (ry > 1) EO_put_integer(edif_stream, ry);
//							EO_close_block(edif_stream, x_("array"));
//						}
//	
//						if (ni->proto->primindex != 0)
//						{
//							EO_open_block(edif_stream, x_("viewRef"));
//							EO_put_identifier(edif_stream, x_("cell"));
//							EO_open_block(edif_stream, x_("cellRef"));
//							if (fun == NPGATEAND || fun == NPGATEOR || fun == NPGATEXOR)
//							{
//								// count the number of inputs
//								i = 0;
//								for (pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//									if (pi->proto == ni->proto->firstportproto) i++;
//								(void)esnprintf(name, WORD+1, x_("%s%ld"), io_ediftoken(ni->proto->protoname), i);
//								EO_put_identifier(edif_stream, name);
//							} else EO_put_identifier(edif_stream, io_edifdescribepriminst(ni, fun));
//							EO_put_block(edif_stream, x_("libraryRef"), x_("lib0"));
//							EO_close_block(edif_stream, x_("viewRef"));
//						} else if (ni->proto->cellview == el_iconview &&
//							contentsview(ni->proto) == NONODEPROTO)
//						{
//							// this node came from an external schematic library
//							EO_open_block(edif_stream, x_("viewRef"));
//							EO_put_identifier(edif_stream, x_("cell"));
//							EO_open_block(edif_stream, x_("cellRef"));
//							EO_put_identifier(edif_stream, io_ediftoken(ni->proto->protoname));
//							(void)esnprintf(name, WORD+1, x_("schem_lib_%ld"), ni->proto->temp2);
//							EO_put_block(edif_stream, x_("libraryRef"), name);
//							EO_close_block(edif_stream, x_("viewRef"));
//						} else
//						{
//							// this node came from this library
//							EO_open_block(edif_stream, x_("viewRef"));
//							EO_put_identifier(edif_stream, x_("cell"));
//							EO_open_block(edif_stream, x_("cellRef"));
//							EO_put_identifier(edif_stream, io_ediftoken(ni->proto->protoname));
//							EO_put_block(edif_stream, x_("libraryRef"), np->lib->libname);
//							EO_close_block(edif_stream, x_("viewRef"));
//						}
//	
//						// now graphical information
//						if (schematic_view)
//						{
//							EO_open_block(edif_stream, x_("transform"));
//	
//							// get the orientation (note only support orthogonal)
//							EO_put_block(edif_stream, x_("orientation"), io_edif_orientation(ni));
//	
//							// now the origin
//							EO_open_block(edif_stream, x_("origin"));
//							var = getvalkey((INTBIG) ni->proto, VNODEPROTO, VINTEGER | VISARRAY,
//								el_prototype_center_key);
//							if (var != NOVARIABLE)
//							{
//								bx = ((INTBIG *) var->addr)[0] + (ni->lowx + ni->highx) / 2 -
//									(ni->proto->lowx + ni->proto->highx) / 2;
//								by = ((INTBIG *) var->addr)[1] + (ni->lowy + ni->highy) / 2 -
//									(ni->proto->lowy + ni->proto->highy) / 2;
//							} else
//							{
//								// use center of node
//								// now origin, normal placement
//								bx = (ni->lowx - ni->proto->lowx);
//								by = (ni->lowy - ni->proto->lowy);
//							}
//							makerot(ni, trans);
//							xform(bx, by, &xpos, &ypos, trans);
//							io_edif_pt(xpos, ypos);
//							EO_close_block(edif_stream, x_("transform"));
//						}
//	
//	
//						// check for variables to write as properties
//						if (schematic_view)
//						{
//							// do all display variables first
//							displaytotal = tech_displayablenvars(ni, NOWINDOWPART, &tech_oneprocpolyloop);
//							for (i = 0; i < displaytotal; i++)
//							{
//								var = tech_filldisplayablenvar(ni, poly, NOWINDOWPART, 0, &tech_oneprocpolyloop);
//								xformpoly(poly, trans);
//								// check for name
//								if (namesame((pt = makename(var->key)), x_("EDIF_annotate")))
//								{
//									// open the property (all properties are strings at this time)
//									EO_open_block(edif_stream, x_("property"));
//									EO_put_identifier(edif_stream, pt);
//									EO_open_block(edif_stream, x_("string"));
//								} else
//								{
//									EO_open_block(edif_stream, x_("annotate"));
//									pt = NULL;
//								}
//								io_edifsymbol_showpoly(poly);
//								if (pt != NULL) EO_close_block(edif_stream, x_("property")); else
//									EO_close_block(edif_stream, x_("annotate"));
//							}
//						}
//						EO_close_block(edif_stream, x_("instance"));
//					}
//	
//					// search for unconnected inputs
//					for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					{
//						if (ni->proto->primindex != 0)
//						{
//							fun = nodefunction(ni);
//							if (fun != NPUNKNOWN && fun != NPPIN && fun != NPCONTACT &&
//								fun != NPNODE && fun != NPCONNECT && fun != NPART) continue;
//						} else if (namesame(ni->proto->protoname, x_("monitor_probe")) == 0) continue;
//	
//						// ignore recursive references (showing icon in contents)
//						if (isiconof(ni->proto, np)) continue;
//	
//						for (pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//						{
//							if ((pp->userbits & STATEBITS) == INPORT || (pp->userbits & STATEBITS) == REFINPORT)
//							{
//								onet = NONETWORK;
//								for (pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//									if (pi->proto == pp) break;
//	
//								if (pi != NOPORTARCINST) onet = pi->conarcinst->network; else
//								{
//									for (pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//										if (pe->proto == pp) break;
//									if (pe != NOPORTEXPINST) onet = pe->exportproto->network;
//								}
//	
//								if (onet == NONETWORK)
//									ttyputmsg(_("** WARNING - no connection to %s port %s on %s in %s"),
//										(((pp->userbits & STATEBITS) == INPORT) ? _("input") : _("vbias")),
//											pp->protoname, describenodeinst(ni),
//												describenodeproto(ni->parent));
//								else if (onet->buswidth < pp->network->buswidth)
//									for (i = onet->buswidth; i != pp->network->buswidth; i++)
//										ttyputmsg(_("** WARNING - no connection to %s port %s (signal %ld) on %s in %s"),
//											((pp->userbits & STATEBITS) == INPORT ? _("input") : _("vbias")),
//								 				networkname(pp->network->networklist[i], 0), i,
//													 describenodeinst(ni), describenodeproto(ni->parent));
//							}
//						}
//					}
//	
//					transid(trans);
//	
//					// if there is anything to connect, write the networks in the cell
//					for (net = np->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//					{
//						// skip bus networks altogether (they are done wire by wire)
//						if (net->buswidth > 1)
//						{
//							// handle bus description, note that most nets have single arc description which is handled below
//							// evaluate the bus name, look for net arrays
//							if (net->namecount > 0)
//							{
//								// EMPTY 
//							}
//							EO_open_block(edif_stream, x_("netBundle"));
//							if (net->namecount > 0)
//							{
//								pt = networkname(net, 0);
//								oname = io_ediftoken(pt);
//								if (namesame(oname, pt))
//								{
//									// different names
//									EO_open_block(edif_stream, x_("rename"));
//									EO_put_identifier(edif_stream, oname);
//									EO_put_string(edif_stream, pt);
//									EO_close_block(edif_stream, x_("rename"));
//								} else EO_put_identifier(edif_stream, oname);
//							} else
//							{
//								net->temp1 = netindex++;
//								(void)esnprintf(line, WORD+1, x_("BUS%ld"), net->temp1);
//								EO_put_identifier(edif_stream, line);
//							}
//							EO_open_block(edif_stream, x_("listOfNets"));
//	
//							// now each sub-net name
//							for (i = 0; i < net->buswidth; i++)
//							{
//								EO_open_block(edif_stream, x_("net"));
//	
//								// now output this name
//								if (net->networklist[i]->namecount > 0)
//								{
//									pt = networkname(net, 0);
//									oname = io_ediftoken(pt);
//									if (namesame(oname, pt))
//									{
//										// different names
//										EO_open_block(edif_stream, x_("rename"));
//										EO_put_identifier(edif_stream, oname);
//										EO_put_string(edif_stream, pt);
//										EO_close_block(edif_stream, x_("rename"));
//									} else EO_put_identifier(edif_stream, oname);
//									(void)setvalkey((INTBIG)net, VNETWORK, EDIF_name_key, (INTBIG)line, VSTRING);
//								} else
//								{
//									if (net->networklist[i]->temp1 != 0)
//										net->networklist[i]->temp1 = netindex++;
//									(void)esnprintf(line, WORD+1, x_("NET%ld"), net->networklist[i]->temp1);
//									(void)setvalkey((INTBIG)net, VNETWORK, EDIF_name_key, (INTBIG)line, VSTRING);
//									EO_put_identifier(edif_stream, line);
//								}
//								EO_close_block(edif_stream, x_("net"));
//							}
//	
//							// now graphics for the bus
//							if (schematic_view)
//							{
//								// output net graphic information
//								// output all arc instances connected to this net
//								egraphic = EGUNKNOWN;
//								egraphic_override = EGBUS;
//								for (ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//									if (ai->network == net) io_edifsymbol_arcinst(ai, trans);
//								io_edifsetgraphic(EGUNKNOWN);
//								egraphic_override = EGUNKNOWN;
//							}
//	
//							EO_close_block(edif_stream, x_("netBundle"));
//							continue;
//						}
//	
//						// skip networks that are not connected to anything real
//						if (net->buslinkcount == 0 && net->portcount == 0)
//						{
//							for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//							{
//								if (ni->proto->primindex == 0 && namesame(ni->proto->protoname,
//									x_("monitor_probe")) == 0) continue;
//	
//								for (pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//									if (pi->conarcinst->network == net) break;
//	
//								if (pi == NOPORTARCINST)
//								{
//									for (pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//										if (pe->exportproto->network == net) break;
//									if (pe == NOPORTEXPINST) continue;
//								}
//								if (ni->proto->primindex == 0) break;
//	
//								fun = nodefunction(ni);
//								if (fun != NPUNKNOWN && fun != NPPIN && fun != NPCONTACT &&
//									fun != NPNODE && fun != NPCONNECT && fun != NPART) break;
//							}
//							if (ni == NONODEINST) continue;
//						}
//	
//						// establish if this is a global net
//						globalport = FALSE;
//						if ((pp = (PORTPROTO *) net->temp2) != NOPORTPROTO)
//							globalport = io_edifisglobal(pp);
//	
//						EO_open_block(edif_stream, x_("net"));
//						if (net->namecount > 0)
//						{
//							pt = networkname(net, 0);
//							if (globalport)
//							{
//								EO_open_block(edif_stream, x_("rename"));
//								EO_put_identifier(edif_stream, io_ediftoken(pt));
//								(void)esnprintf(name, WORD+1, x_("%s!"), io_ediftoken(pt));
//								EO_put_identifier(edif_stream, name);
//								EO_close_block(edif_stream, x_("rename"));
//								EO_put_block(edif_stream, x_("property"), x_("GLOBAL"));
//							} else
//							{
//								oname = io_ediftoken(pt);
//								if (namesame(oname, pt))
//								{
//									// different names
//									EO_open_block(edif_stream, x_("rename"));
//									EO_put_identifier(edif_stream, oname);
//									EO_put_string(edif_stream, pt);
//									EO_close_block(edif_stream, x_("rename"));
//								} else EO_put_identifier(edif_stream, oname);
//								(void)setvalkey((INTBIG)net, VNETWORK, EDIF_name_key, (INTBIG)line, VSTRING);
//							}
//						} else
//						{
//							net->temp1 = netindex++;
//							(void)esnprintf(line, WORD+1, x_("NET%ld"), net->temp1);
//							(void)setvalkey((INTBIG)net, VNETWORK, EDIF_name_key, (INTBIG)line, VSTRING);
//							EO_put_identifier(edif_stream, line);
//						}
//	
//						// write net connections
//						EO_open_block(edif_stream, x_("joined"));
//	
//						// include exported ports (by net name)
//						if (pp != NOPORTPROTO && !globalport)
//							EO_put_block(edif_stream, x_("portRef"), io_ediftoken(networkname(net, 0)));
//	
//						// now include components using existing net-port pointers
//						for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//						{
//							if (ni->proto->primindex != 0)
//							{
//								// ignore passive components
//								fun = nodefunction(ni);
//								if (fun == NPUNKNOWN || fun == NPPIN || fun == NPCONTACT ||
//									fun == NPNODE || fun == NPCONNECT || fun == NPART) continue;
//								cnp = ni->proto;
//							} else
//							{
//								// ignore recursive references (showing icon in contents)
//								if (isiconof(ni->proto, np)) continue;
//	
//								if (namesame(ni->proto->protoname, x_("monitor_probe")) == 0)
//									continue;
//								fun = NPUNKNOWN;
//	
//								// get contents of the nodeinst to establish net connections
//								if ((cnp = contentsview(ni->proto)) == NONODEPROTO) cnp = ni->proto;
//							}
//	
//							// be sure each connection is written only once
//							for (cpp = cnp->firstportproto; cpp != NOPORTPROTO; cpp = cpp->nextportproto)
//								cpp->temp1 = 0;
//	
//							// write connection to ports exported directly
//							for (pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//							{
//								if (pe->exportproto != NOPORTPROTO && pe->exportproto->network == net)
//								{
//									// locate the name being used
//									if ((cpp = equivalentport(ni->proto, pe->proto, cnp)) == NOPORTPROTO)
//										cpp = pe->proto;
//									if (ni->proto->primindex == 0 && (PORTPROTO *)(cpp->network->temp2) != NOPORTPROTO)
//										cpp = (PORTPROTO *)(cpp->network->temp2);
//									if (cpp->temp1++ != 0) continue;
//	
//									if (ni->proto->primindex == 0) pt = networkname(cpp->network, 0); else
//										pt = cpp->protoname;
//	
//									EO_open_block(edif_stream, x_("portRef"));
//									EO_put_identifier(edif_stream, io_ediftoken(pt));
//									EO_put_block(edif_stream, x_("instanceRef"), io_edifwritecompname(ni, fun, 0));
//									EO_close_block(edif_stream, x_("portRef"));
//									cpp->temp1++;
//								}
//							}
//	
//							// write single-wire direct connections
//							for (pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								if ((pi->conarcinst->network == net))
//								{
//									if ((cpp = equivalentport(ni->proto, pi->proto, cnp)) == NOPORTPROTO)
//										cpp = pi->proto;
//	
//									if (net_buswidth(pi->proto->protoname) == 1)
//									{
//										if (ni->proto->primindex == 0 && (PORTPROTO *)(cpp->network->temp2) != NOPORTPROTO)
//											cpp = (PORTPROTO *)(cpp->network->temp2);
//										if (cpp->temp1++ != 0) continue;
//	
//										if (ni->proto->primindex == 0) pt = networkname(cpp->network, 0); else
//											pt = cpp->protoname;
//										EO_open_block(edif_stream, x_("portRef"));
//										EO_put_identifier(edif_stream, io_ediftoken(pt));
//										EO_put_block(edif_stream, x_("instanceRef"), io_edifwritecompname(ni, fun, 0));
//										EO_close_block(edif_stream, x_("portRef"));
//									} else
//									{
//										// connect to first signal in the bus
//										if (ni->proto->primindex == 0)
//										{
//											EO_open_block(edif_stream, x_("portRef"));
//											EO_put_identifier(edif_stream,
//												io_ediftoken(networkname(pi->proto->network->networklist[0],0)));
//											EO_open_block(edif_stream, x_("portRef"));
//											EO_put_block(edif_stream, x_("instanceRef"),
//												io_edifwritecompname(ni, fun, 0));
//											EO_close_block(edif_stream, x_("portRef"));
//										} else ttyputerr(_("Cannot handle primitives with bus pins"));
//									}
//									cpp->temp1++;
//								}
//							}
//	
//							// match up exported net with bus connections on this nodeinst
//							for (pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								if ((netcount = pi->conarcinst->network->buswidth) > 1)
//								{
//									// first, find the connection point if there is one
//									for (i = 0; i < netcount; i++)
//										if (pi->conarcinst->network->networklist[i] == net) break;
//									if (i == netcount) continue;
//	
//									if ((cpp = equivalentport(ni->proto, pi->proto, cnp)) == NOPORTPROTO)
//										cpp = pi->proto;
//	
//									// skip if already connected
//									if (cpp->temp1 != 0) continue;
//	
//									// associate by the i-th position in the connection
//									EO_open_block(edif_stream, x_("portRef"));
//									if (cpp->network->buswidth > i)
//									{
//										if (cpp->network->buswidth > 1)
//											cnet = cpp->network->networklist[i]; else
//												cnet = cpp->network;
//	
//										// now transform to the port identification network
//										cpp = (PORTPROTO *) cnet->temp2;
//	
//										// skip if already connected
//										if (cpp->temp1++ != 0) continue;
//										EO_put_identifier(edif_stream, io_ediftoken(networkname(cnet, 0)));
//									} else
//									{
//										pt = networkname(cpp->network, 0);
//										ttyputerr(_("Proto bus width too narrow at %s {signal %ld} in %s"),
//											pt, i, describenodeproto(cpp->parent));
//										EO_put_identifier(edif_stream,
//											io_ediftoken(pt));
//									}
//									EO_put_block(edif_stream, x_("instanceRef"),
//										io_edifwritecompname(ni, fun, 0));
//									EO_close_block(edif_stream, x_("portRef"));
//								}
//							}
//	
//							// continue with connected busses
//							for (pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//							{
//								if ((netcount = pe->exportproto->network->buswidth) > 1)
//								{
//									// first, find the connection point if there is one
//									for (i = 0; i < netcount; i++)
//										if (pe->exportproto->network->networklist[i] == net) break;
//									if (i == netcount) continue;
//	
//									if ((cpp = equivalentport(ni->proto, pe->proto, cnp)) == NOPORTPROTO)
//										cpp = pe->proto;
//	
//									// associate by the i-th position in the connection
//									EO_open_block(edif_stream, x_("portRef"));
//									if (cpp->network->buswidth > i)
//									{
//										if (cpp->network->buswidth > 1)
//											EO_put_identifier(edif_stream,
//												io_ediftoken(networkname(cpp->network->networklist[i], 0)));
//										else EO_put_identifier(edif_stream,
//											io_ediftoken(networkname(cpp->network, 0)));
//									} else
//									{
//										pt = networkname(cpp->network, 0);
//										ttyputerr(_("Proto bus width too narrow at %s"),
//											pt);
//										EO_put_identifier(edif_stream,
//											io_ediftoken(pt));
//									}
//									EO_put_block(edif_stream, x_("instanceRef"),
//										io_edifwritecompname(ni, fun, 0));
//									EO_close_block(edif_stream, x_("portRef"));
//	
//									cpp->temp1++;
//								}
//							}
//						} // for ni = ...
//						EO_close_block(edif_stream, x_("joined"));
//	
//						if (schematic_view)
//						{
//							// output net graphic information
//							// output all arc instances connected to this net
//							egraphic = EGUNKNOWN;
//							egraphic_override = EGWIRE;
//							for (ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//								if (ai->network == net) io_edifsymbol_arcinst(ai, trans);
//							io_edifsetgraphic(EGUNKNOWN);
//							egraphic_override = EGUNKNOWN;
//						}
//	
//						if (globalport)
//							EO_put_block(edif_stream, x_("userData"), x_("global"));
//						EO_close_block(edif_stream, x_("net"));
//					} // for (net = ...
//					if (schematic_view && schematic)
//						EO_close_block(edif_stream, x_("page"));
//				}	// if np->firstnodeinst != NONODEINST
//	
//				if (schematic)
//				{
//					// get next schematic
//					pageindex++;
//					(void)esnprintf(page, 10, x_("P%ld"), pageindex);
//					FOR_CELLGROUP(lnp, np)
//						if (!namesame(lnp->cellview->sviewname, page))
//					{
//						// list all ports in interface except global ports in network order
//						(void)io_edifmarknetports(lnp);
//						break;
//					}
//					if (np == NONODEPROTO) break;
//				} else break;
//			} // while np != NONODEPROTO
//		}
//	
//		// matches "(cell "
//		EO_close_block(edif_stream, x_("cell"));
//		return(backannotate);
//	}
//	
//	/*
//	 * procedure to properly identify an instance of a primitive node
//	 * for ASPECT netlists
//	 */
//	CHAR *io_edifdescribepriminst(NODEINST *ni, INTBIG fun)
//	{
//		REGISTER VARIABLE *var;
//		CHAR *model;
//	
//		switch (fun)
//		{
//			case NPRESIST:
//				var = getvalkey((INTBIG) ni, VNODEINST, VSTRING, sch_spicemodelkey);
//				if (var == NOVARIABLE) return(x_("Resistor"));
//	
//				model = (CHAR *) var->addr;
//				if (namesamen(model, x_("PN"), 2) == 0) return(x_("res_pnpoly"));
//				if (namesamen(model, x_("NP"), 2) == 0) return(x_("res_nppoly"));
//				if (namesamen(model, x_("PP"), 2) == 0) return(x_("res_pppoly"));
//				if (namesamen(model, x_("BL"), 2) == 0) return(x_("res_bl"));
//				if (namesamen(model, x_("EP"), 2) == 0) return(x_("res_epi"));
//				return(x_("Resistor"));
//			case NPTRANPN:
//			case NPTRANSREF:
//				return(x_("npn"));
//			case NPTRAPNP:
//				return(x_("pnp"));
//			case NPSUBSTRATE:
//				return(x_("gtap"));
//			case NPDIODE:
//				var = getvalkey((INTBIG) ni, VNODEINST, VSTRING, sch_spicemodelkey);
//				if (var != NOVARIABLE && namesamen(x_("subtap"), (CHAR *) var->addr, 6) == 0)
//					return(x_("gtap"));
//		}
//		return(io_ediftoken(ni->proto->protoname));
//	}
//	
//	/*
//	 * Establish whether port 'pp' is a global port or not
//	 */
//	BOOLEAN io_edifisglobal(PORTPROTO *pp)
//	{
//		NODEPROTO *inp;
//	
//		// pp is a global port if it is marked global
//		if ((pp->userbits & BODYONLY) != 0) return(TRUE);
//	
//		// or if it does not exist on the icon
//		if ((inp = iconview(pp->parent)) == NONODEPROTO) return(FALSE);
//		if (equivalentport(pp->parent, pp, inp) == NOPORTPROTO) return(TRUE);
//		return(FALSE);
//	}
//	
//	/*
//	 * routine to mark all nets' temp2 variables for interfacing purposes.
//	 * To determine the connection, temp2 is marked with a pointer to the
//	 * port which exports the net.  Returns the number of nets exported.
//	 * Note: only single-wire nets are marked; all bus nets' temp2 are
//	 * marked NOPORTPROTO.  Multiply exported nets have been collapsed at
//	 * both levels of hierarchy to a single net by the network maintainer.
//	 */
//	INTBIG io_edifmarknetports(NODEPROTO *np)
//	{
//		NETWORK *net;
//		PORTPROTO *pp;
//		REGISTER INTBIG i, count, portnetcount;
//	
//		for (net = np->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//			net->temp2 = (INTBIG) NOPORTPROTO;
//	
//		// initialize count of exported individual nets
//		portnetcount = 0;
//		for (pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			// mark the nets that were exported with a pointer to their first port
//			if ((count = pp->network->buswidth) > 1)
//				for (i = 0; i < count; i++)
//			{
//				if (pp->network->networklist[i]->temp2 == (INTBIG)NOPORTPROTO)
//				{
//					pp->network->networklist[i]->temp2 = (INTBIG)pp;
//					portnetcount++;
//				}
//			} else if (pp->network->temp2 == (INTBIG)NOPORTPROTO)
//			{
//				pp->network->temp2 = (INTBIG)pp;
//				portnetcount++;
//			}
//		}
//		return(portnetcount);
//	}
//	
//	/*
//	 * function to incorporate external library interface data or reference file
//	 * for current cell into EDIF netlist - returns false if successful
//	 */
//	BOOLEAN io_edifwritelibext(NODEPROTO *np)
//	{
//		REGISTER FILE *f;
//		UCHAR1 buf[256];
//		CHAR *filename;
//		REGISTER INTBIG count;
//		REGISTER VARIABLE *var;
//	
//		// import the external library for this cell, if it exists
//		var = getval((INTBIG) np, VNODEPROTO, VSTRING, x_("EDIF_external_lib"));
//		if (var == NOVARIABLE) return(TRUE);
//		f = xopen((CHAR *)var->addr, io_filetypeedif, x_(""), &filename);
//		if (f == NULL)
//		{
//			ttyputerr(_("Cannot find EDIF external reference file %s on cell %s"),
//				(CHAR *) var->addr, describenodeproto(np));
//			return(TRUE);
//		}
//		for (;;)
//		{			// copy the file
//			count = xfread(buf, 1, 256, f);
//			if (count <= 0) break;
//			if (xfwrite(buf, 1, count, edif_stream->file) != count)
//			{
//				ttyputerr(_("Error copying EDIF reference file %s"), (CHAR *) var->addr);
//				xclose(f);
//				return(TRUE);
//			}
//		}
//		xclose(f);
//		ttyputmsg(_("Incorporated external EDIF reference file %s"), (CHAR *) var->addr);
//		return(FALSE);
//	}
//	
//	/*
//	 * Returns 0 there is no valid name in "var", corrected name if valid.
//	 */
//	CHAR *io_edifvalidname(VARIABLE *var)
//	{
//		CHAR *iptr;
//		static CHAR name[WORD+1];
//	
//		if (var == NOVARIABLE) return(0);
//		estrcpy(name, (CHAR *)var->addr);
//		if (estrlen(name) == 0) return(0);
//		iptr = name;
//	
//		// allow '&' for the first character (this must be fixed latter if digit or '_')
//		if (*iptr == '&') iptr++;
//	
//		// allow _ and alphanumeric for others
//		for (iptr++; *iptr != 0; iptr++)
//			if (*iptr != '_' && !(*iptr >= 'a' && *iptr <= 'z') && !(*iptr >= 'A' && *iptr <= 'Z') &&
//				!(*iptr >= '0' && *iptr <= '9')) *iptr = '_';
//	
//		return(name);
//	}
//	
//	/*
//	 * sometimes all the characters have to be lower case
//	 */
//	CHAR *io_ediflowerstring(CHAR *str)
//	{
//		CHAR *pt;
//		static CHAR newstr[1000];
//		INTBIG i;
//	
//		for (pt = str, i = 0; *pt != 0; i++)
//			newstr[i] = (isupper(*pt) ? tolower(*pt++) : *pt++);
//		newstr[i] = '\0';
//		return(newstr);
//	}
//	
//	/*
//	 * sometimes they all have to be upper case
//	 */
//	CHAR *io_edifupperstring(CHAR *str)
//	{
//		CHAR *pt;
//		static CHAR newstr[1000];
//		INTBIG i;
//	
//		for (pt = str, i = 0; *pt != 0; i++)
//			newstr[i] = (islower(*pt) ? toupper(*pt++) : *pt++);
//		newstr[i] = '\0';
//		return(newstr);
//	}
//	
//	/* module: io_edifsymbol
//	 * function: will output all graphic objects of a symbol (extracted from
//	 * us_drawcell).
//	 */
//	void io_edifsymbol(NODEPROTO *np)
//	{
//		EO_open_block(edif_stream, x_("symbol"));
//		egraphic_override = EGWIRE;
//		egraphic = EGUNKNOWN;
//		for (pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			EO_open_block(edif_stream, x_("portImplementation"));
//			EO_put_identifier(edif_stream, io_ediftoken(pp->protoname));
//			EO_open_block(edif_stream, x_("connectLocation"));
//			shapeportpoly(pp->subnodeinst, pp->subportproto, poly, FALSE);
//			io_edifsymbol_showpoly(poly);
//	
//			// close figure
//			io_edifsetgraphic(EGUNKNOWN);
//			EO_close_block(edif_stream, x_("portImplementation"));
//		}
//		egraphic_override = EGUNKNOWN;
//	
//		// create the identity transform for this window
//		transid(trans);
//	
//		for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			io_edifsymbol_cell(ni, trans);
//		}
//		for (ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//		{
//			io_edifsymbol_arcinst(ai, trans);
//		}
//	
//		// close figure
//		io_edifsetgraphic(EGUNKNOWN);
//		EO_close_block(edif_stream, x_("symbol"));
//	}
//	
//	/* module: io_edifsymbol_cell
//	 * function: will output a specific symbol cell
//	 */
//	void io_edifsymbol_cell(NODEINST *ni, XARRAY prevtrans)
//	{
//		XARRAY localtran, trans;
//	
//		// make transformation matrix within the current nodeinst
//		if (ni->rotation == 0 && ni->transpose == 0)
//		{
//			io_edifsymbol_nodeinst(ni, prevtrans);
//		} else
//		{
//			makerot(ni, localtran);
//			transmult(localtran, prevtrans, trans);
//			io_edifsymbol_nodeinst(ni, trans);
//		}
//	}
//	
//	/*
//	 * routine to symbol "ni" when transformed through "prevtrans".
//	 */
//	void io_edifsymbol_nodeinst(NODEINST *ni, XARRAY prevtrans)
//	{
//		INTBIG i, j, displaytotal, low, high, istext;
//		CHAR *name;
//		XARRAY localtran, subrot, trans;
//		INTBIG bx, by, ux, uy, swap;
//		static POLYGON *poly = NOPOLYGON;
//		GRAPHICS *gra;
//		NODEPROTO *np;
//		PORTPROTO *pp;
//		PORTEXPINST *pe;
//		NODEINST *ino;
//		ARCINST *iar;
//		VARIABLE *var;
//	
//		// get polygon
//		(void)needstaticpolygon(&poly, 4, io_tool->cluster);
//	
//		np = ni->proto;
//	
//		// get outline of nodeinst in the window
//		xform(ni->lowx, ni->lowy, &bx, &by, prevtrans);
//		xform(ni->highx, ni->highy, &ux, &uy, prevtrans);
//		if (bx > ux)
//		{
//			swap = bx;
//			bx = ux;
//			ux = swap;
//		}
//		if (by > uy)
//		{
//			swap = by;
//			by = uy;
//			uy = swap;
//		}
//	
//		// write port names if appropriate
//		if (ni->firstportexpinst != NOPORTEXPINST)
//		{
//			for (pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//			{
//				// us_writeprotoname(pe->exportproto, on, prevtrans, LAYERO,
//				// el_colcelltxt&on, w, 0, 0, 0, 0);
//			}
//		}
//	
//		// primitive nodeinst: ask the technology how to draw it
//		if (np->primindex != 0)
//		{
//			high = nodepolys(ni, 0, NOWINDOWPART);
//	
//			// don't draw invisible pins
//			if (np == gen_invispinprim) low = 1; else
//				low = 0;
//	
//			for (j = low; j < high; j++)
//			{
//				// get description of this layer
//				shapenodepoly(ni, j, poly);
//	
//				// ignore if this layer is not being displayed
//				gra = poly->desc;
//				if ((gra->colstyle & INVISIBLE) != 0) continue;
//	
//				// draw the nodeinst
//				xformpoly(poly, prevtrans);
//	
//				// draw the nodeinst and restore the color
//				// check for text ...
//				if (poly->style >= TEXTCENT && poly->style <= TEXTBOX)
//				{
//					istext = 1;
//					// close the current figure ...
//					io_edifsetgraphic(EGUNKNOWN);
//					EO_open_block(edif_stream, x_("annotate"));
//				} else istext = 0;
//	
//				(void)io_edifsymbol_showpoly(poly);
//				if (istext) EO_close_block(edif_stream, x_("annotate"));
//			}
//		} else
//		{
//			// transform into the nodeinst for display of its guts
//			maketrans(ni, localtran);
//			transmult(localtran, prevtrans, subrot);
//	
//			// get cell rectangle
//			maketruerectpoly(ni->lowx, ni->highx, ni->lowy, ni->highy, poly);
//			poly->style = CLOSEDRECT;
//			xformpoly(poly, prevtrans);
//	
//			// write ports that must always be displayed
//			for (pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				if ((pp->userbits & PORTDRAWN) != 0)
//				{
//					if (pp->subnodeinst->rotation == 0 && pp->subnodeinst->transpose == 0)
//					{
//						// EMPTY 
//						// us_writeprotoname(pp, LAYERA, subrot, LAYERO, el_colcelltxt, w, portcliplx, portcliphx, portcliply, portcliphy);
//					} else
//					{
//						makerot(pp->subnodeinst, localtran);
//						transmult(localtran, subrot, trans);
//						// us_writeprotoname(pp, LAYERA, trans, LAYERO, el_colcelltxt, w, portcliplx, portcliphx, portcliply, portcliphy);
//					}
//				}
//			}
//	
//			// see if there are displayable variables on the cell
//			if ((displaytotal = tech_displayablenvars(ni, NOWINDOWPART, &tech_oneprocpolyloop)) != 0)
//				io_edifsetgraphic(EGUNKNOWN);
//			for (i = 0; i < displaytotal; i++)
//			{
//				var = tech_filldisplayablenvar(ni, poly, NOWINDOWPART, 0, &tech_oneprocpolyloop);
//				xformpoly(poly, prevtrans);
//				// check for name
//				if (namesame((name = makename(var->key)), x_("EDIF_annotate")))
//				{
//					// open the property
//					EO_open_block(edif_stream, x_("property"));
//					EO_put_identifier(edif_stream, name);
//					EO_open_block(edif_stream, x_("string"));
//				} else
//				{
//					EO_open_block(edif_stream, x_("annotate"));
//					name = NULL;
//				}
//				io_edifsymbol_showpoly(poly);
//				if (name != NULL) EO_close_block(edif_stream, x_("property")); else
//					EO_close_block(edif_stream, x_("annotate"));
//			}
//	
//			// search through cell
//			for (ino = np->firstnodeinst; ino != NONODEINST; ino = ino->nextnodeinst)
//			{
//				io_edifsymbol_cell(ino, subrot);
//			}
//			for (iar = np->firstarcinst; iar != NOARCINST; iar = iar->nextarcinst)
//			{
//				io_edifsymbol_arcinst(iar, subrot);
//			}
//		}
//	}
//	
//	/*
//	 * routine to draw an arcinst.  Returns indicator of what else needs to
//	 * be drawn.  Returns negative if display interrupted
//	 */
//	void io_edifsymbol_arcinst(ARCINST *ai, XARRAY trans)
//	{
//		INTBIG i, j, displaytotal;
//		INTBIG width;
//		VARIABLE *var;
//		CHAR *name;
//		static POLYGON *poly = NOPOLYGON;
//	
//		// ask the technology how to draw the arcinst
//		// get polygon
//		(void)needstaticpolygon(&poly, 4, io_tool->cluster);
//	
//		i = arcpolys(ai, NOWINDOWPART);
//	
//		// get the endpoints of the arcinst
//		for (j = 0; j < i; j++)
//		{
//			// generate a polygon, force line for path generation
//			width = ai->width;
//			ai->width = 0;
//			shapearcpoly(ai, j, poly);
//			ai->width = width;
//	
//			// check for text (all arcs should not have text), do variables below
//			if (poly->style >= TEXTCENT && poly->style <= TEXTBOX) break;
//	
//			// draw the arcinst
//			xformpoly(poly, trans);
//	
//			// draw the arcinst and restore the color
//			io_edifsymbol_showpoly(poly);
//		}
//	
//		// now get the variables
//		displaytotal = tech_displayableavars(ai, NOWINDOWPART, &tech_oneprocpolyloop);
//		if (displaytotal != 0) io_edifsetgraphic(EGUNKNOWN);
//		for (i = 0; i < displaytotal; i++)
//		{
//			var = tech_filldisplayableavar(ai, poly, NOWINDOWPART, 0, &tech_oneprocpolyloop);
//			xformpoly(poly, trans);
//	
//			// check for name
//			if (namesame((name = makename(var->key)), x_("EDIF_annotate")))
//			{
//				// open the property
//				EO_open_block(edif_stream, x_("property"));
//				EO_put_identifier(edif_stream, name);
//				EO_open_block(edif_stream, x_("string"));
//			} else
//			{
//				EO_open_block(edif_stream, x_("annotate"));
//				name = NULL;
//			}
//			io_edifsymbol_showpoly(poly);
//			if (name != NULL) EO_close_block(edif_stream, x_("property")); else
//			EO_close_block(edif_stream, x_("annotate"));
//		}
//	}
//	
//	void io_edifsetgraphic(_egraphic type)
//	{
//		if (type == EGUNKNOWN)
//		{
//			// terminate the figure
//			if (egraphic != EGUNKNOWN) EO_close_block(edif_stream, x_("figure"));
//			egraphic = EGUNKNOWN;
//		} else if (egraphic_override == EGUNKNOWN)
//		{
//			// normal case
//			if (type != egraphic)
//			{
//				// new egraphic type
//				if (egraphic != EGUNKNOWN) EO_close_block(edif_stream, x_("figure"));
//				egraphic = type;
//				EO_open_block(edif_stream, x_("figure"));
//				EO_put_identifier(edif_stream, egraphic_text[egraphic]);
//			}
//		} else if (egraphic != egraphic_override)
//		{
//			// override figure
//			if (egraphic != EGUNKNOWN) EO_close_block(edif_stream, x_("figure"));
//			egraphic = egraphic_override;
//			EO_open_block(edif_stream, x_("figure"));
//			EO_put_identifier(edif_stream, egraphic_text[egraphic]);
//		}
//	}
//	
//	/* module: io_edifsymbol_showpoly
//	 * function: will write polys into EDIF syntax
//	 * inputs: poly
//	 */
//	INTBIG io_edifsymbol_showpoly(POLYGON *obj)
//	{
//		INTBIG i, lx, ux, ly, uy, six, siy, height;
//	
//		// now draw the polygon
//		switch (obj->style)
//		{
//			case CIRCLE:
//			case DISC:			// a circle
//				io_edifsetgraphic(EGART);
//				i = computedistance(obj->xv[0], obj->yv[0], obj->xv[1], obj->yv[1]);
//				EO_open_block(edif_stream, x_("circle"));
//				io_edif_pt(obj->xv[0] - i, obj->yv[0]);
//				io_edif_pt(obj->xv[0] + i, obj->yv[0]);
//				EO_close_block(edif_stream, x_("circle"));
//				return(0);
//	
//			case CIRCLEARC:
//				io_edifsetgraphic(EGART);
//	
//				// arcs at [i] points [1+i] [2+i] clockwise
//				if (obj->count == 0) return(1);
//				if ((obj->count % 3) != 0) return(1);
//				for (i = 0; i < obj->count; i += 3)
//				{
//					EO_open_block(edif_stream, x_("openShape"));
//					EO_open_block(edif_stream, x_("curve"));
//					EO_open_block(edif_stream, x_("arc"));
//					io_edif_pt(obj->xv[i + 1], obj->yv[i + 1]);
//	
//					// calculate a point between the first and second point
//					io_compute_center(obj->xv[i], obj->yv[i],
//						obj->xv[i + 1], obj->yv[i + 1], obj->xv[i + 2], obj->yv[i + 2], &six, &siy);
//					io_edif_pt(six, siy);
//					io_edif_pt(obj->xv[i + 2], obj->yv[i + 2]);
//					EO_close_block(edif_stream, x_("openShape"));
//				}
//				return(0);
//	
//			case FILLED:			// filled polygon
//			case FILLEDRECT:		// filled rectangle
//			case CLOSED:			// closed polygon outline
//			case CLOSEDRECT:		// closed rectangle outline
//				if (isbox(obj, &lx, &ux, &ly, &uy))
//				{
//					// simple rectangular box
//					if (lx == ux && ly == uy)
//					{
//						if (egraphic_override == EGUNKNOWN) return(0);
//						io_edifsetgraphic(EGART);
//						EO_open_block(edif_stream, x_("dot"));
//						io_edif_pt(lx, ly);
//						EO_close_block(edif_stream, x_("dot"));
//					} else
//					{
//						io_edifsetgraphic(EGART);
//						EO_open_block(edif_stream, x_("rectangle"));
//						io_edif_pt(lx, ly);
//						io_edif_pt(ux, uy);
//						EO_close_block(edif_stream, x_("rectangle"));
//					}
//				} else
//				{
//					io_edifsetgraphic(EGART);
//					EO_open_block(edif_stream, x_("path"));
//					EO_open_block(edif_stream, x_("pointList"));
//					for (i = 0; i < obj->count; i++)
//						io_edif_pt(obj->xv[i], obj->yv[i]);
//					if (obj->count > 2) io_edif_pt(obj->xv[0], obj->yv[0]);
//					EO_close_block(edif_stream, x_("path"));
//				}
//				return(0);
//	
//			case TEXTCENT:		// text centered in box
//			case TEXTTOP:		// text below top of box
//			case TEXTBOT:		// text above bottom of box
//			case TEXTLEFT:		// text right of left edge of box
//			case TEXTRIGHT:		// text left of right edge of box
//			case TEXTTOPLEFT:		// text to lower-right of upper-left corner
//			case TEXTBOTLEFT:		// text to upper-right of lower-left corner
//			case TEXTTOPRIGHT:		// text to lower-left of upper-right corner
//			case TEXTBOTRIGHT:		// text to upper-left of lower-right corner
//				getbbox(obj, &lx, &ux, &ly, &uy);
//				io_edifsetgraphic(EGUNKNOWN);
//				EO_open_block(edif_stream, x_("stringDisplay"));
//				EO_put_string(edif_stream, obj->string);
//				EO_open_block(edif_stream, x_("display"));
//				if (TXTGETPOINTS(TDGETSIZE(obj->textdescript)) != 0)
//				{
//					EO_open_block(edif_stream, x_("figureGroupOverride"));
//					EO_put_identifier(edif_stream, egraphic_text[EGART]);
//	
//					// output the text height
//					EO_open_block(edif_stream, x_("textHeight"));
//	
//					// 2 pixels = 0.0278 in or 36 double pixels per inch
//					height = muldiv((el_curlib->lambda[el_curtech->techindex] * 10),
//						TXTGETPOINTS(TDGETSIZE(obj->textdescript)), 36);
//					EO_put_integer(edif_stream, io_edif_scale(height));
//					EO_close_block(edif_stream, x_("figureGroupOverride"));
//				} else EO_put_identifier(edif_stream, egraphic_text[EGART]);
//				switch (obj->style)
//				{
//					case TEXTCENT:
//						EO_put_block(edif_stream, x_("justify"), x_("CENTERCENTER"));
//						break;
//					case TEXTTOP:		// text below top of box
//						EO_put_block(edif_stream, x_("justify"), x_("LOWERCENTER"));
//						break;
//					case TEXTBOT:		// text above bottom of box
//						EO_put_block(edif_stream, x_("justify"), x_("UPPERCENTER"));
//						break;
//					case TEXTLEFT:		// text right of left edge of box
//						EO_put_block(edif_stream, x_("justify"), x_("CENTERRIGHT"));
//						break;
//					case TEXTRIGHT:		// text left of right edge of box
//						EO_put_block(edif_stream, x_("justify"), x_("CENTERLEFT"));
//						break;
//					case TEXTTOPLEFT:		// text to lower-right of upper-left corner
//						EO_put_block(edif_stream, x_("justify"), x_("LOWERRIGHT"));
//						break;
//					case TEXTBOTLEFT:		// text to upper-right of lower-left corner
//						EO_put_block(edif_stream, x_("justify"), x_("UPPERRIGHT"));
//						break;
//					case TEXTTOPRIGHT:		// text to lower-left of upper-right corner
//						EO_put_block(edif_stream, x_("justify"), x_("LOWERLEFT"));
//						break;
//					case TEXTBOTRIGHT:		// text to upper-left of lower-right corner
//						EO_put_block(edif_stream, x_("justify"), x_("UPPERLEFT"));
//						break;
//				}
//				EO_put_block(edif_stream, x_("orientation"), x_("R0"));
//				EO_open_block(edif_stream, x_("origin"));
//				io_edif_pt(lx, ly);
//				EO_close_block(edif_stream, x_("stringDisplay"));
//				return(0);
//	
//			case TEXTBOX:		// text centered and contained in box
//				getbbox(obj, &lx, &ux, &ly, &uy);
//				return(0);
//	
//			case OPENED:		// opened polygon outline
//			case OPENEDT1:		// opened polygon outline, texture 1
//			case OPENEDT2:		// opened polygon outline, texture 2
//			case OPENEDT3:		// opened polygon outline, texture 3
//			case OPENEDO1:		// extended opened polygon outline
//				// check for closed 4 sided figure
//				if (obj->count == 5 && obj->xv[4] == obj->xv[0] && obj->yv[4] == obj->yv[0])
//				{
//					obj->count = 4;
//					i = obj->style;
//					obj->style = CLOSED;
//					if (isbox(obj, &lx, &ux, &ly, &uy))
//					{
//						// simple rectangular box
//						if (lx == ux && ly == uy)
//						{
//							if (egraphic_override == EGUNKNOWN) return(0);
//							io_edifsetgraphic(EGART);
//							EO_open_block(edif_stream, x_("dot"));
//							io_edif_pt(lx, ly);
//							EO_close_block(edif_stream, x_("dot"));
//						} else
//						{
//							io_edifsetgraphic(EGART);
//							EO_open_block(edif_stream, x_("rectangle"));
//							io_edif_pt(lx, ly);
//							io_edif_pt(ux, uy);
//							EO_close_block(edif_stream, x_("rectangle"));
//						}
//						obj->count = 5;
//						obj->style = i;
//						return(0);
//					}
//					obj->count = 5;
//					obj->style = i;
//				}
//				io_edifsetgraphic(EGART);
//				EO_open_block(edif_stream, x_("path"));
//				EO_open_block(edif_stream, x_("pointList"));
//				for (i = 0; i < obj->count; i++)
//					io_edif_pt(obj->xv[i], obj->yv[i]);
//				EO_close_block(edif_stream, x_("path"));
//				return(0);
//	
//			case VECTORS:
//				io_edifsetgraphic(EGART);
//				for (i = 0; i < obj->count; i += 2)
//				{
//					EO_open_block(edif_stream, x_("path"));
//					EO_open_block(edif_stream, x_("pointList"));
//					io_edif_pt(obj->xv[i], obj->yv[i]);
//					io_edif_pt(obj->xv[i + 1], obj->yv[i + 1]);
//					EO_close_block(edif_stream, x_("path"));
//				}
//				return(0);
//	
//			// unsupported operators
//			case CROSS:			// crosses (always have one point)
//				getcenter(obj, &six, &siy);
//				return(0);
//		}
//	
//		// unknown polygon type
//		return(1);
//	}
//	
//	/******** OUTPUT SUPPORT ********/
//	
//	/* module: EO_open_stream
//	   function:  Will create a stream block for a new edif file, this stream
//	   block is used for all future references.
//	   inputs:
//	   filename - the name of the file to open
//	   compress - the compress file or pretty-print
//	   returns:  The new stream block
//	 */
//	EO_STREAM_PTR EO_open_stream(CHAR *filename, INTBIG compress)
//	{
//		EO_STREAM_PTR stream;
//		CHAR *truename;
//	
//		// get a new stream
//		if ((stream = EO_alloc_stream()) == NOEO_STREAM) return(NOEO_STREAM);
//	
//		// open the file
//		if ((stream->file = xcreate(filename, io_filetypeedif, _("EDIF File"), &truename)) == NULL)
//		{
//			if (truename != 0)
//			{
//				ttyputerr(_("edifout: could not create stream <%s>"), truename);
//				ttyputerr(_("Cannot write %s"), truename);
//			}
//			(void)EO_free_stream(stream);
//			return(NOEO_STREAM);
//		}
//	
//		// update filename - allocate name and initialize the structure
//		if (allocstring(&stream->filename, truename, io_tool->cluster))
//		{
//			ttyputnomemory();
//			return(NOEO_STREAM);
//		}
//	
//		stream->state = EO_OPENED;
//		stream->compress = compress;
//		return(stream);
//	}
//	
//	/* module:  EO_close_stream
//	   function:  Will close a stream file, and terminate all currently open
//	   blocks.
//	   returns: 0 on success, 1 on error
//	 */
//	INTBIG EO_close_stream(EO_STREAM_PTR stream)
//	{
//		if (stream != NOEO_STREAM)
//		{
//			if (stream->blkstack_ptr)
//			{
//				if (EO_close_block(stream, stream->blkstack[0]))
//				ttyputerr(_("edifout: internal error, closing stream <%s>"), stream->filename);
//			}
//			if (stream->state == EO_OPENED)
//			{
//				xprintf(stream->file, x_("\n"));
//				xclose(stream->file);
//			}
//			(void)EO_free_stream(stream);
//			return(0);
//		}
//		return(1);
//	}
//	
//	/* module:  EO_open_block
//	   function:  Will open a new keyword block, will indent the new block
//	   depending on depth of the keyword
//	   returns: 0 on success, 1 on error
//	 */
//	INTBIG EO_open_block(EO_STREAM_PTR stream, CHAR *keyword)
//	{
//		if (stream != NOEO_STREAM && keyword != NULL)
//		{
//			// output the new block
//			if (stream->blkstack_ptr)
//			{
//				xprintf(stream->file, x_("\n"));
//			}
//			if (allocstring(&stream->blkstack[stream->blkstack_ptr++], keyword,
//				io_tool->cluster))
//			{
//				ttyputnomemory();
//				return(1);
//			}
//	
//			// output the keyword
//			xprintf(stream->file, x_("%s(%s%s"),
//				GETBLANKS(stream->compress, stream->blkstack_ptr-1), DOSPACE, keyword);
//			return(0);
//		}
//		return(1);
//	}
//	
//	/* module: EO_put_block
//	   function:  Will output a one identifier block
//	   returns: 0 on success, 1 on error
//	 */
//	INTBIG EO_put_block(EO_STREAM_PTR stream, CHAR *keyword, CHAR *identifier)
//	{
//		if (stream != NOEO_STREAM && keyword != NULL)
//		{
//			// output the new block
//			if (stream->blkstack_ptr)
//			{
//				(void)efprintf(stream->file, x_("\n"));
//			}
//	
//			// output the keyword
//			xprintf(stream->file, x_("%s(%s%s %s%s)"),
//				GETBLANKS(stream->compress, stream->blkstack_ptr), DOSPACE, keyword,
//					identifier, DOSPACE);
//			return(0);
//		}
//		return(1);
//	}
//	
//	/* module: EO_put_identifier
//	   function:  Will output a string identifier to the stream file
//	   returns: 0 on success, 1 on error
//	 */
//	INTBIG EO_put_identifier(EO_STREAM_PTR stream, CHAR *str)
//	{
//		if (stream != NOEO_STREAM && str != NULL)
//		{
//			xprintf(stream->file, x_(" %s"), str);
//			return(0);
//		}
//		return(1);
//	}
//	
//	/* module: EO_put_string
//	   function:  Will output a quoted string to the stream file
//	   returns: 0 on success, 1 on error
//	 */
//	INTBIG EO_put_string(EO_STREAM_PTR stream, CHAR *str)
//	{
//		if (stream != NOEO_STREAM && str != NULL)
//		{
//			xprintf(stream->file, x_(" \"%s\""), str);
//			return(0);
//		}
//		return(1);
//	}
//	
//	/* module: EO_put_integer
//	   function:  Will output an integer to the stream edif file
//	   returns: 0 on success, 1 on error
//	 */
//	INTBIG EO_put_integer(EO_STREAM_PTR stream, INTBIG val)
//	{
//		if (stream != NOEO_STREAM)
//		{
//			xprintf(stream->file, x_(" %ld"), val);
//			return(0);
//		}
//		return(1);
//	}
//	
//	/* module: EO_put_float
//	   function:  Will output an integer to the stream edif file
//	   returns: 0 on success, 1 on error
//	 */
//	INTBIG EO_put_float(EO_STREAM_PTR stream, double val)
//	{
//		if (stream != NOEO_STREAM)
//		{
//			xprintf(stream->file, x_("%s(%se %s%s)"), DOSPACE, DOSPACE, EO_get_exp(val), DOSPACE);
//			return(0);
//		}
//		return(1);
//	}
//	
//	INTBIG EO_close_block(EO_STREAM_PTR stream, CHAR *keyword)
//	{
//		INTBIG depth;
//	
//		if (stream != NOEO_STREAM)
//		{
//			if (stream->blkstack_ptr == 0) return(0);
//			if (keyword == NULL)
//			{
//				depth = 1;
//			} else
//			{
//				// scan for this saved keyword
//				for (depth = 1; depth <= stream->blkstack_ptr; depth++)
//				{
//					if (!namesame(stream->blkstack[stream->blkstack_ptr - depth ], keyword)) break;
//				}
//				if (depth > stream->blkstack_ptr)
//				{
//					ttyputerr(_("edifout: could not match keyword <%s>"), keyword);
//					return(1);
//				}
//			}
//	
//			// now terminate and free keyword list
//			do
//			{
//				efree(stream->blkstack[--stream->blkstack_ptr]);
//				xprintf(stream->file, x_("%s)"), DOSPACE);
//			} while (--depth);
//			return(0);
//		}
//		return(1);
//	}
//	
//	static EO_STREAM_PTR EO_stream_active = NOEO_STREAM;
//	
//	EO_STREAM_PTR EO_alloc_stream(void)
//	{
//		EO_STREAM_PTR stream;
//	
//		if ((stream = (EO_STREAM_PTR)emalloc(sizeof (EO_STREAM), io_tool->cluster)) == NOEO_STREAM)
//		{
//			ttyputnomemory();
//			return(NOEO_STREAM);
//		}
//	
//		// now initialize the structure
//		stream->filename = NULL;
//		stream->file = NULL;
//		stream->state = EO_CLOSED;
//		stream->fpos = 0;
//		stream->blkstack_ptr = 0;
//	
//		// add to the active list
//		stream->next = EO_stream_active;
//		EO_stream_active = stream;
//		return(stream);
//	}
//	
//	INTBIG EO_free_stream(EO_STREAM_PTR stream)
//	{
//		EO_STREAM_PTR temp;
//	
//		if (stream == NOEO_STREAM)
//		{
//			ttyputerr(_("edifout: internal error, no stream block"));
//			return(1);
//		}
//	
//		// remove from the active list
//		if (stream == EO_stream_active) EO_stream_active = stream->next; else
//		{
//			// scan for this stream
//			for (temp = EO_stream_active; temp != NOEO_STREAM; temp = temp->next)
//			{
//				if (temp->next == stream)
//				{
//					temp->next = stream->next;
//					break;
//				}
//			}
//			if (temp == NOEO_STREAM)
//			{
//				ttyputerr(_("edifout: internal error, can't find stream <%s>"),
//					(stream->filename?stream->filename:x_("noname")));
//			}
//		}
//		if (stream->filename != NULL)
//		{
//			efree(stream->filename);
//			stream->filename = NULL;
//		}
//		efree((CHAR *)stream);
//		return(0);
//	}
//	
//	CHAR *EO_get_timestamp(void)
//	{
//		static CHAR get_timestamp_buf[81];
//		time_t t;
//		CHAR month[4];
//		unsigned short mon, yr, day, hour, min, sec;
//	
//		t = getcurrenttime();
//		(void)esscanf(timetostring(t), x_("%*s %s %hd %hd:%hd:%hd %hd"), &month[0], &day, &hour,
//			&min, &sec, &yr);
//	
//		mon = (unsigned short)parsemonth(month);
//	
//		// now make the time string
//		(void)esnprintf(get_timestamp_buf, 81, x_("%d %02d %02d %02d %02d %02d"),
//			yr, mon, day, hour, min, sec);
//		return(get_timestamp_buf);
//	}
//	
//	/* module: EO_make_string
//	   function: Will add quotes to a string
//	   returns: new string
//	 */
//	CHAR *EO_make_string(CHAR *str)
//	{
//		static CHAR newstr[LINE+1];
//	
//		(void)esnprintf(newstr, LINE+1, x_("\"%s\""), str);
//		return(newstr);
//	}
//	
//	/* module: EO_get_exp
//	   function:  Will expand an floating point number to a integer matissa and
//	   exponent
//	   inputs:
//	   val - the double to convert
//	   returns a pointer to the integer string
//	 */
//	CHAR *EO_get_exp(double val)
//	{
//		static CHAR result[WORD+1];
//		CHAR temp[WORD+1], *pp, *rp;
//		INTBIG nonzero, cnt, exp;
//	
//		// first generate an expanded value
//		(void)esnprintf(temp, WORD+1, x_("%9e"), val);
//	
//		// now parse out the result
//		pp = temp; rp = result;
//		while (*pp != '.') *rp++ = *pp++;
//	
//		// now the rest of the matissa
//		nonzero = cnt = 0;
//		pp++;
//		while (*pp != 'e')
//		{
//			*rp++ = *pp;
//			cnt++;
//			if (*pp != '0') nonzero = cnt;
//			pp++;
//		}
//	
//		// now determine the integer conversion factor
//		rp -= (cnt - nonzero);
//		*rp++ = ' ';
//	
//		// now convert the exponent
//		exp = eatoi(++pp);
//		(void)esnprintf(rp, WORD-10, x_("%ld"), exp-nonzero);
//		return(result);
//	}
