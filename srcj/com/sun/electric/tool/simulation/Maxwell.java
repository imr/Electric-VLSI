/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Maxwell.java
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
package com.sun.electric.tool.simulation;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

/**
 * @author Willy Chung
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Maxwell 
{	
	public class Maxnet
	{
		int globalnetnumber;
		Integer[] layerlist;
		int layercount;
		int layertotal;
		Maxnet nextmaxnet;
	}
	
	public class WriteMaxwell extends Job
	{
		Maxnet sim_maxwell_firstmaxnet;
		Maxnet sim_maxwell_maxnetfree = null;
		Variable sim_maxwell_var;
		int sim_maxwell_boxnumber;
		int sim_maxwell_netnumber;	
		private NodeProto nodeProto;
		
		protected WriteMaxwell(NodeProto np)
		{
			super("Maxwell Simulation", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			nodeProto = np;
			this.startJob();			
		}
		
		public void doIt()
		{
			this.SimWriteMaxwell(nodeProto);	
		}
		
		public void SimWriteMaxwell(NodeProto np)
		{
			String truename, name, temp;
			int i;
			Maxnet mn, nextmn;
			JNetwork net;
			
			name=np.getProtoName();
			name.concat(".mac");
			
			File outputFile = new File(name);
			
			truename = outputFile.getAbsolutePath();
			
			if(!outputFile.canWrite())
			{
				System.out.println("Error writing file '"+truename+"'");
				return;
			}
			
			try
			{
				FileWriter writeFile = new FileWriter(outputFile);
				BufferedWriter writeLine = new BufferedWriter(writeFile);
			
				temp = "# Maxwel for Cell "+np.describe()+" from library "+np.whichCell().getLibrary().getLibName();
				writeLine.write(temp);
				writeLine.newLine();
			
				if(true)//usoption!=nodateornoversion)
				{
					temp = new String(Integer.toString(np.whichCell().getVersion()));
					if(temp.equals(""))
					{
						temp = "# CELL VERSION "+temp;
						writeLine.write(temp);
						writeLine.newLine();
					}
		
					temp = np.whichCell().getCreationDate().toString();
					if(temp.equals(""))
					{
						temp = "# CELL CREATED ON "+temp;
						writeLine.write(temp);
						writeLine.newLine();
					}
								
					temp = np.whichCell().getRevisionDate().toString();
					if(temp.equals(""))
					{
						temp = "# LAST REVISED ON"+temp;
						writeLine.write(temp);
						writeLine.newLine();
					}
					
					temp = "# Maxwell netlist written by Electric Design System; version ";//+ElectricVersion
					writeLine.write(temp);
					writeLine.newLine();
			
					temp = "# WRITTEN ON "+ DateFormat.getDateInstance().format(new Date());
					writeLine.write(temp);
					writeLine.newLine();
				}
				else
				{
					writeLine.write("# Maxwell netlist written by Electric Design System");
					writeLine.newLine();
				}
				
				writeLine.newLine();
				
				sim_maxwell_var=User.tool.getVar("key"/*getvalkey*/);
	
				sim_maxwell_boxnumber =1;
				sim_maxwell_netnumber =0;
				
// 				Cell.rebuildAllNetworks(null);
// 				Iterator it = ((Cell)np).getNetworks();
				
// 				while(it.hasNext())
// 				{
// 					net=(JNetwork)it.next();
// 					//net.temp1=sim_maxwell_netnumber++;
// 				}
				
				sim_maxwell_firstmaxnet=null;
				SimWriteMaxCell(np,writeLine,null/*el_matid??*/ );
				
				for(mn=sim_maxwell_firstmaxnet; mn!=null; mn=nextmn)
				{
					nextmn=mn.nextmaxnet;
					
					if(mn.layercount>1)
					{
						writeLine.write("Unite {");
						for(i=0; i<mn.layercount;i++)
						{
							if(i!=0)
							{
								writeLine.write(" ");
							}
							writeLine.write("\"Box-"+mn.layerlist[i]+"\"");
						}
						writeLine.newLine();
					}
				}
				
				System.out.println(truename+" writen");
				writeLine.close();
				
			}catch(IOException e)
			{
				System.out.println("Error wrting file '"+truename+"'");
				return;
			}
			
				
		}
		
		public void SimWriteMaxCell(NodeProto np, BufferedWriter io, AffineTransform trans)
		{
			int i, tot = 0;
			NodeInst ni = null;
			ArcInst ai;
			JNetwork net;
			Connection pi = null;
			Export pe;
			AffineTransform transr = null, transt, temptrans, subrot;
			Poly poly = null;
			
	//		if(stop)
	//		{
	//			stop
	//			return;
	//		}
			
			//need static polygon
			Iterator niIt=np.getInstancesOf();
			while(niIt.hasNext())
			{
				ni = (NodeInst)niIt.next();
				transr = ni.rotateOut();
				temptrans = ni.translateOut(trans);
				temptrans = ni.rotateOut(temptrans);
				
				if(ni.getProto().getTechnology().isNoPrimitiveNodes()==false)
				{
					transt = ni.translateOut();
					subrot = ni.translateOut(temptrans);
					subrot = ni.rotateOut(subrot);
					
// 					Cell.rebuildAllNetworks(null);		
// 					Iterator it = ((Cell)ni.getProto()).getUserNetlist().getNetworks();
				
// 					while(it.hasNext())
// 					{
// 						net=(JNetwork)it.next();
// 						//net.temp1=-1;
// 					}
					Iterator it = ni.getConnections();
					while(it.hasNext())
					{
						pi=(Connection)it.next();
						//pi->proto->network->temp1=pi->conarcinst->network->temp1;
					}
					it=ni.getExports();
					while(it.hasNext())
					{	
						pe = (Export)it.next();
						//pe->proto->network->temp1 = pe->exportproto->network->temp1;
					}
// 					Cell.rebuildAllNetworks(null);
// 					it = ((Cell)ni.getProto()).getUserNetlist().getNetworks();
// 					while(it.hasNext())
// 					{
// 						net=(JNetwork)it.next();
// 	//					if(net->temp1==-1)
// 	//					{
// 	//						net->temp1=sim_maxwell_netnumber++;
// 	//					}
// 					}
					SimWriteMaxCell(ni.getProto(), io, subrot);
				}
				else
				{
					//tot=ni.nodeEpolys(ni,0,nowindowpart);
					for(i=0;i<tot;i++)
					{
						poly = ni.getShapeOfPort((PortProto)ni.getProto().findPortProto(ni.getName()));
						if(poly.getPort()==null)
						{
							continue;
						}
						Iterator it = ni.getConnections();
						while(it.hasNext())
						{
							pi =(Connection)it.next();
							if(pi.getPortInst().getPortProto()==poly.getPort())
							{
								break;
							}
						}
						if(pi==null)
						{
							continue;
						}
						poly.transform(temptrans);
						SimWriteMaxPoly(poly, io, 0/*pi->conarcinst->network->temp1*/);
					}
				}	
			}
		
			//for(ai=np->firstarcinst;ai!=null; ai=ai->nextarcinst)
			//{
				//tot = ai.arcpolys(ai,nowindowpart);
			//	for(i=0; i<tot; i++)
			//	{
				//	poly = shapearcpoly(ai.i.poly);
					poly.transform(trans);
					SimWriteMaxPoly(poly, io, 0/*ai->network->temp1*/);
				//}
			//}
		}
		
	
		public void SimWriteMaxPoly(Poly poly, BufferedWriter io, int globalnet)
		{
			int r = 0,g = 0,b = 0,i, newtotal;
			Layer.Function fun;
			double x = 0,y = 0, dx = 0, dy = 0;
			Vector newlist;
			Maxnet mn;
			String layname;
			String laynamebot = null, laynamehei = null, thisname = null; 
			
			if(poly.getPort().getParent().getTechnology()!=Technology.getCurrent())
			{
				return;
			}
			
			fun = poly.getLayer().getFunction();
			
			//if(fun.!=Layer.Function.PSEUDO)
			//{
			//	return;
			//}
			
			Rectangle2D rec = poly.getBox(); 
			
			if(rec==null)
			{
				return;
			}
			
			try {	
				if(sim_maxwell_var!=null)
				{
					//rgb=color???
					
						io.write("NewObjColor "+r+" "+g+" "+b);
					
				}	
	//			x = scaletodispunit(lx, DISPUNITMIC);
				
	//			y = scaletodispunit(ly, DISPUNITMIC);
	//			dx = scaletodispunit(hx-lx, DISPUNITMIC);
	//			dy = scaletodispunit(hy-ly, DISPUNITMIC);
				layname = poly.getLayer().getName();
				laynamebot = layname+"-Bot";
				laynamehei = layname+"-Hei";
			
				/* find this layer and add maxwell box */
				for(mn = sim_maxwell_firstmaxnet; mn != null; mn = mn.nextmaxnet)
				{
					if (mn.globalnetnumber == globalnet) 
					{	
						break; 
					} 
				}
				if (mn == null)
				{
					mn = new Maxnet();
					if (mn == null)
					{
						 return;
					}
					mn.nextmaxnet = sim_maxwell_firstmaxnet;
					sim_maxwell_firstmaxnet = mn;
					mn.globalnetnumber = globalnet;
				}
	
				/* now add this box to the maxnet */
				if (mn.layercount >= mn.layertotal)
				{
					newtotal = mn.layertotal * 2;
					if (mn.layercount >= newtotal) 
					{
						newtotal = mn.layercount + 20;
					}
					newlist = new Vector();
					if (newlist == null)
					{
						return;
					}
					for(i=0; i<mn.layercount; i++)
					{
						newlist.add(mn.layerlist[i]);
					}
					mn.layerlist =(Integer[])newlist.toArray();
					mn.layertotal = newtotal;
				}
				mn.layerlist[mn.layercount++] = new Integer(sim_maxwell_boxnumber);
		
				thisname = "Box-"+Integer.toString(sim_maxwell_boxnumber);
				sim_maxwell_boxnumber++;
		
				io.write("Box pos3 "+x+" "+y+" "+laynamebot+" "+dx+" "+dy+ " "+laynamehei+"\""+thisname+"\"");
				io.newLine();
	
			
			} catch (IOException e)
			{
				System.out.println("Failed Writing");
			}
		}
	}
	
	public void SimWriteMaxwell(NodeProto np)
	{
		WriteMaxwell max = new WriteMaxwell(np);
	}
}
