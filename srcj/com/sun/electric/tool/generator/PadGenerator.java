/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PadGenerator.java
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
package com.sun.electric.tool.generator;

import java.awt.geom.Point2D;
import java.io.*;
import java.util.StringTokenizer;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.DialogOpenFile;

/**
 * @author Willy Chung
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PadGenerator
{
	public class ArrayAlign
	{
		String cell;
		String inport;
		String outport;
		ArrayAlign nextArrayAlign;
	}
	
	public class PortAssociate
	{
		NodeInst ni;
		PortProto pp;
		PortProto corepp;
		PortAssociate nextportassociate;
	}

	public class PadFrame extends Job
	{
		String filename;
		
		protected PadFrame(String file)
		{
			super("Pad Frame Generator", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.filename = file;
			this.startJob();
		}
		
	
		public void doIt()
		{
			String lineRead, keyWord, truename, save, libname, cellname, exportname = null;
			int copycells = 0;
			int angle =0;
			int lineno;
			int gap =0, gapx =0, gapy = 0;
			
			PortProto pp = null, exportpp;
			Library lib, savelib;
			NodeProto np, corenp;
			Cell cell;
			NodeInst ni = null, lastni;
			ArcInst ai = null;
			Technology savetech;
			ArrayAlign aa, firstaa;
			PortAssociate pa, firstpa;
			Input.ImportType style;
			Poly poly;
			double centerX = 0;
			double centerY = 0;
			int filetypearray = -1;
			double width = 0, height = 0;
						
			File inputFile = new File(this.filename);
			
			if(inputFile==null||inputFile.canRead()==false)
			{
				System.out.println("Error reading file");
				return;
			}
			
			FileReader readFile;
			try {
				readFile = new FileReader(inputFile);
			
				BufferedReader readLine  = new BufferedReader(readFile);
				StringTokenizer str;
	
				lineRead = readLine.readLine();
		
				firstaa = null;
				firstpa = null;
				lineno=1;
				angle = 0;
				copycells=0;
				cell = null;
				corenp = null;
				lastni = null;
				lib = null;
				
				while(lineRead!=null)
				{
					str = new StringTokenizer(lineRead, " \t");
					if(str.hasMoreTokens())
					{
						keyWord = str.nextToken();

						if(keyWord.charAt(0)!=';')
						{
							do
							{
								if(keyWord.equals("celllibrary"))
								{
									if(str.hasMoreTokens())
									{
										keyWord=str.nextToken();
										style=Input.ImportType.BINARY;
										String tmp[] = keyWord.split("\\.");
									
										if(tmp[1]=="txt")
										{
											style=Input.ImportType.TEXT;
										}
										libname = tmp[0];
										lib=Library.findLibrary(libname);
										
										if(lib==null)
										{
											lib = Library.newInstance(libname, inputFile.getName());
											lib = Input.readLibrary(lib.getLibName(), style);
											if(lib==null)
											{
												System.out.println("Line "+lineno+": cannot read library " + keyWord);
												return;
											}									
										}
									}

									if(str.hasMoreTokens())
									{
										keyWord=str.nextToken();
										if(keyWord.equals("copy"))
										{
											copycells=1;
										}
									}
									continue;
								}
								else if (keyWord.equals("facet"))
								{
									if(str.hasMoreTokens())
									{
										keyWord=str.nextToken();
										cell = Cell.newInstance(Library.getCurrent(), keyWord);
										
										if(cell==null)
										{
											System.out.println("Line "+lineno+": unable to create cell " + keyWord);
											break;
										}
									}			
									continue;			
								}
								else if (keyWord.equals("core"))
								{
									if(str.hasMoreTokens())
									{
										keyWord=str.nextToken();
										corenp=NodeProto.findNodeProto(keyWord);
										
										if(corenp==null)
										{
											System.out.println("Line "+lineno+": cannot find core cell "+keyWord);
										}
									}
									continue;
								}
								else if(keyWord.equals("rotate"))
								{
									if(str.hasMoreTokens())
									{
										keyWord=str.nextToken();
										if(keyWord.equals("c"))
										{
											angle = (angle+2700)%3600;
										}
										else if(keyWord.equals("cc"))
										{
											angle = (angle+900)%3600;
										}
										else
										{
											System.out.println("Line "+lineno+": incorrect rotation "+keyWord);
										}
									}
									continue;
								}
								else if(keyWord.equals("align"))
								{
									aa = new ArrayAlign();
									keyWord = str.nextToken();
									
									if(keyWord.equals(""))
									{
										System.out.println("Line "+lineno+": missing 'cell' name");
										break;
									}
									aa.cell = keyWord;
									
									keyWord = str.nextToken();
									
									if(keyWord.equals(""))
									{
										System.out.println("Line "+lineno+": missing 'in port' name");
										break;					
									}
									aa.inport = keyWord;
									
									keyWord = str.nextToken();
									
									if(keyWord.equals(""))
									{
										System.out.println("Line "+lineno+": missing 'out port' name");	
										break;
									}
									aa.outport = keyWord;
									
									aa.nextArrayAlign = firstaa;
									firstaa = aa;
									continue;
								}
								else if(keyWord.equals("place"))
								{
									keyWord = str.nextToken();
									
									if(cell==null)
									{
										System.out.println("Line "+lineno+": no 'facet' line specified for 'place'");
										break;
									}
									
									if(copycells!=0)
									{
										np = NodeProto.findNodeProto(keyWord);
								
										if(np==null && lib==null && lib!=Library.getCurrent())
										{
											savelib = Library.getCurrent();
											Library.setCurrent(lib);
											np=NodeProto.findNodeProto(keyWord);
											Library.setCurrent(savelib);
											
											if(np!=null)
											{
												//copy
												np = CircuitChanges.copyRecursively((Cell)np, np.getProtoName(), Library.getCurrent(),
													((Cell)np).getView(), false, false, "", false, false, false);
											}
										}
									}
									else
									{
										np = NodeProto.findNodeProto(lib.getLibName());
									}
									if(np == null)
									{
										System.out.println("Line "+lineno+": cannot find cell '"+keyWord+"'");
										break;
									}
									
									gap=0;

									exportpp = null;

									while(str.hasMoreTokens())
									{
										keyWord = str.nextToken();
										String temp, temp2;
										if(keyWord.indexOf("=")!=-1)
										{
											temp=keyWord.substring(0,keyWord.indexOf("=")-1);										
										}
										else
										{
											temp= keyWord;									
										}
										
										if(temp.equals("gap"))
										{
											if(keyWord.indexOf("=")!=-1)
											{
												if(keyWord.substring(keyWord.indexOf("=")+1)=="")
												{
													temp2 = str.nextToken();
												}
												else
												{
													temp2 = keyWord.substring(keyWord.indexOf("=")+1);
												}
											}
											else
											{
												keyWord=str.nextToken();
											
												if(keyWord.charAt(0)=='=')
												{
													temp2 = keyWord.substring(1);										
												}
												else
												{
													System.out.println("Line "+lineno+": missing '=' after 'gap'");
													break;
												}
											}
											
											gap = Integer.parseInt(temp2);
										}
										else if(temp.equals("export"))
										{
											if(!str.hasMoreTokens())
											{
												System.out.println("Line "+lineno+": missing port name after 'export'");
												break;
											}
											
											keyWord=str.nextToken();
											
											if(keyWord.indexOf("=")!=-1)
											{
												temp=keyWord.substring(0,keyWord.indexOf("=")-1);
												if(keyWord.substring(keyWord.indexOf("=")+1)=="")
												{
													temp2 = str.nextToken();
												}
												else
												{
													temp2 = keyWord.substring(keyWord.indexOf("=")+1);
												}										
											}
											else
											{
												temp= keyWord;
												keyWord=str.nextToken();
											
												if(keyWord.charAt(0)=='=')
												{
													temp2 = keyWord.substring(1);										
												}
												else
												{
													System.out.println("Line "+lineno+": missing '=' after 'export PORT'");
													break;
												}									
											}
											
											exportpp = np.findPortProto(temp);
											if(exportpp==null)
											{
												System.out.println("Line "+lineno+": no port '"+temp+"' on cell '"+temp+"'");
												break;
											}
											
											exportname = temp2;
											
											if(exportname.equals(""))
											{
												System.out.println("Line "+lineno+": missing export name after 'export PORT='");
												break;
											}	
										}
										else
										{
											pa=new PortAssociate();
											pa.ni=null;
											pa.pp=np.findPortProto(temp);
											if(pa.pp==null)
											{
												System.out.println("Line "+lineno+": no port '"+temp+"' on cell '"+temp+"'");
											}
											
											if(keyWord.indexOf("=")!=-1)
											{
												if(keyWord.substring(keyWord.indexOf("=")+1)=="")
												{
													temp2 = str.nextToken();
												}
												else
												{
													temp2 = keyWord.substring(keyWord.indexOf("=")+1);
												}
											}
											else
											{
												keyWord=str.nextToken();
		
												if(keyWord.charAt(0)=='=')
												{
													temp2 = keyWord.substring(1);										
												}
												else
												{
													System.out.println("Line "+lineno+": missing '=' after pad port name");
													break;
												}
											}
											
											if(corenp==null)
											{
												System.out.println("Line "+lineno+": no core cell for association");
											}
											pa.corepp=corenp.findPortProto(temp2);
											
											if(pa.corepp==null)
											{
												System.out.println("Line "+lineno+": no port '"+temp2+"' on cell '"+temp2+"'");
												break;
											}
											pa.nextportassociate=firstpa;
											firstpa=pa;
										}
									}					
									if(lastni!=null)
									{
										cellname=(lastni.getProtoEquivalent()).noLibDescribe();
										for(aa=firstaa; aa!=null; aa=aa.nextArrayAlign)
										{
											if(aa.cell.equals(cellname))
											{
												break;
											}
										}
										if(aa==null)
										{
											System.out.println("Line "+lineno+": no port alignment given for cell "+lastni.describe());
											break;
										}
										
										pp=(lastni.getProto()).findPortProto(aa.outport); 
										
										if(pp==null)
										{
											System.out.println("Line "+lineno+": no port called '"+aa.outport+"' on cell"+lastni.describe());
											break;
										}
										
										poly = (lastni.findPortInstFromProto(pp)).getPoly();
										centerX = poly.getCenterX();
										centerY = poly.getCenterY();
										width=poly.getBounds().getWidth();
										height=poly.getBounds().getHeight();
										
										cellname=np.whichCell().noLibDescribe();
										
										for(aa=firstaa; aa!=null; aa=aa.nextArrayAlign)
										{
											if(aa.cell.equals(cellname))
											{
												break;
											}
										}						
										if(aa==null)
										{
											System.out.println("Line "+lineno+": no port called '"+aa.outport+"' on cell "+np.describe());
											break;
										}									
										pp=np.findPortProto(aa.inport);
										
										if(pp==null)
										{
											System.out.println("Line "+lineno+": no port called '"+aa.inport+"' on cell "+np.describe());
											break;
										}
									}
									
									//corneroffset(NONODEINST,np,angle,0,&ox,&oy,false);
									Point2D pointCenter = new Point2D.Double(centerX, centerY);
												
									ni=NodeInst.newInstance(np,pointCenter,width,height, angle, cell, null);
									if(ni==null)
									{
										System.out.println("Line "+lineno+": problem creating"+np.describe()+" instance");
										break;
									}
									
									if(lastni!=null)
									{
										switch(angle)
										{
											case 0:    gapx =  gap;   gapy =    0;   break;
											case 900:  gapx =    0;   gapy =  gap;   break;
											case 1800: gapx = -gap;   gapy =    0;   break;
											case 2700: gapx =    0;   gapy = -gap;   break;
										}
										poly = ni.findPortInstFromProto(pp).getPoly();
										double tempx = centerX - (poly.getCenterX()) - gapx;
										double tempy = centerY - (poly.getCenterY()) - gapy;
										ni.modifyInstance(tempx,tempy,0,0,0);
									}
									if(exportpp!=null)
									{
										pp=Export.newInstance(cell, ni.findPortInstFromProto(exportpp), exportname);
									}
									lastni=ni;
									
									for(pa=firstpa; pa!=null; pa=pa.nextportassociate)
									{
										if(pa.ni==null)
										{
											pa.ni=ni;
										}
									}
									continue;
								}
									
								System.out.println("Line "+lineno+": unknown keyword'"+keyWord+"'");
								break;
								
							}while(str.hasMoreTokens());
						}
					}
//					else
//					{
//						System.out.println("Line "+lineno+ ": too short");
//					}
				
				lineRead = readLine.readLine();
				lineno++;
				}
				if(corenp!=null)
				{
					centerX=cell.getBounds().getCenterX();
					centerY=cell.getBounds().getCenterY();
					Point2D center = new Point2D.Double(centerX, centerY);
					
					EditWindow.gridAlign(center, 1);
					//gridalign
					savetech=Technology.getCurrent(); 
					corenp.getTechnology().setCurrent();

					SizeOffset so = corenp.getSizeOffset();
					width = corenp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
					height = corenp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
					ni = NodeInst.newInstance(corenp, center, width, height, 0, cell, null);
					
					for(pa = firstpa; pa != null; pa = pa.nextportassociate)
					{
						if(pa.ni!=null)
						{
							continue;
						}
						
						PortInst pi1=ni.findPortInstFromProto(pa.corepp);
						PortInst pi2=pa.ni.findPortInstFromProto(pa.pp);
						width = Generic.tech.unrouted_arc.getDefaultWidth();

						ai = ArcInst.newInstance(Generic.tech.unrouted_arc, width, pi1, pi2, null);
					}
				}

				//Par
				//us_editcell
			
			} catch (IOException e1) {}
		}
	}
	
	public void ArrayFromFile()
	{
		DialogOpenFile arrayFileType = new DialogOpenFile("arr", "Pad Generator Array File");
		String fileName = arrayFileType.chooseInputFile(null);
		if (fileName == null)
		{
			System.out.println("File not found");
			return;
		}
		PadFrame padFrame = new PadFrame(fileName);
	}

}

