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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.lib.LibFile;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.InputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
	}

	public class PortAssociate
	{
		NodeInst ni;
		PortProto pp;
		PortProto corepp;
	}

	public class PadFrame extends Job
	{
		String filename;

		protected PadFrame(String file)
		{
			super("Pad Frame Generator", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.filename = file;
			startJob();
		}

		public void doIt()
		{
			String lineRead;
			int gap = 0, gapx = 0, gapy = 0;
			PortProto pp = null, exportpp;
			Cell np;
			double width = 0, height = 0;

			File inputFile = new File(filename);
			if (inputFile == null || !inputFile.canRead())
			{
				System.out.println("Error reading file");
				return;
			}

			int angle = 0;
			try {
				FileReader readFile = new FileReader(inputFile);
				BufferedReader readLine = new BufferedReader(readFile);

				lineRead = readLine.readLine();

				List arrayAlignList = new ArrayList();
				List arrayPortAssociate = new ArrayList();
				int lineno = 1;
				boolean copycells = false;
				Cell cell = null;
				Cell corenp = null;
				NodeInst lastni = null;
				Library cellLib = null;

				while (lineRead != null)
				{
					StringTokenizer str = new StringTokenizer(lineRead, " \t");
					if (str.hasMoreTokens())
					{
						String keyWord = str.nextToken();

						if (keyWord.charAt(0) != ';')
						{
							do
							{
								if (keyWord.equals("celllibrary"))
								{
									if (str.hasMoreTokens())
									{
										keyWord = str.nextToken();
										URL fileURL = TextUtils.makeURLToFile(keyWord);
										cellLib = Library.findLibrary(TextUtils.getFileNameWithoutExtension(fileURL));
										if (cellLib == null)
										{
											// library does not exist: see if file can be found locally
                                            StringBuffer errmsg = new StringBuffer();
											if (TextUtils.getURLStream(fileURL, errmsg) == null)
											{
												// try the Electric library area
												fileURL = LibFile.getLibFile(keyWord);
												if (TextUtils.getURLStream(fileURL, null) == null)
												{
													//System.out.println("Cannot find cell library " + fileURL.getPath());
                                                    System.out.println(errmsg.toString());
													return;
												}
											}

											OpenFile.Type style = OpenFile.Type.ELIB;
											if (TextUtils.getExtension(fileURL).equals("txt")) style = OpenFile.Type.READABLEDUMP;
											Library saveLib = Library.getCurrent();
											cellLib = Library.newInstance(TextUtils.getFileNameWithoutExtension(fileURL), fileURL);
											cellLib = Input.readLibrary(fileURL, style);
											if (cellLib == null)
											{
												System.out.println("Line " + lineno + ": cannot read library " + keyWord);
												return;
											}		
											saveLib.setCurrent();
										}
									}

									if (str.hasMoreTokens())
									{
										keyWord = str.nextToken();
										if (keyWord.equals("copy"))
										{
											copycells = true;
										}
									}
									continue;
								}
								else if (keyWord.equals("facet"))
								{
									if (str.hasMoreTokens())
									{
										keyWord = str.nextToken();
										cell = Cell.makeInstance(Library.getCurrent(), keyWord);
										if (cell == null)
										{
											System.out.println("Line " + lineno + ": unable to create cell " + keyWord);
											break;
										}
									}
									continue;			
								}
								else if (keyWord.equals("core"))
								{
									if (str.hasMoreTokens())
									{
										keyWord = str.nextToken();
										corenp = (Cell)NodeProto.findNodeProto(keyWord);
										if (corenp == null)
										{
											System.out.println("Line " + lineno + ": cannot find core cell " + keyWord);
										}
									}
									continue;
								}
								else if (keyWord.equals("rotate"))
								{
									if (str.hasMoreTokens())
									{
										keyWord = str.nextToken();
										if (keyWord.equals("c"))
										{
											angle = (angle+2700)%3600;
										}
										else if (keyWord.equals("cc"))
										{
											angle = (angle+900)%3600;
										}
										else
										{
											System.out.println("Line " + lineno + ": incorrect rotation " + keyWord);
										}
									}
									continue;
								}
								else if (keyWord.equals("align"))
								{
									ArrayAlign aa = new ArrayAlign();
									keyWord = str.nextToken();

									if (keyWord.equals(""))
									{
										System.out.println("Line " + lineno + ": missing 'cell' name");
										break;
									}
									aa.cell = keyWord;

									keyWord = str.nextToken();

									if (keyWord.equals(""))
									{
										System.out.println("Line " + lineno + ": missing 'in port' name");
										break;					
									}
									aa.inport = keyWord;

									keyWord = str.nextToken();

									if (keyWord.equals(""))
									{
										System.out.println("Line " + lineno + ": missing 'out port' name");	
										break;
									}
									aa.outport = keyWord;
									arrayAlignList.add(aa);
									continue;
								}
								else if (keyWord.equals("place"))
								{
									keyWord = str.nextToken();

									if (cell == null)
									{
										System.out.println("Line " + lineno + ": no 'facet' line specified for 'place'");
										break;
									}

									np = cellLib.findNodeProto(keyWord);
									if (copycells)
									{
										if (np != null)
										{
											// copy into the current library
											np = CircuitChanges.copyRecursively(np, np.getProtoName(), Library.getCurrent(),
												np.getView(), false, false, "", false, false, false);
										}
									}
									if (np == null)
									{
										System.out.println("Line " + lineno + ": cannot find cell '" + keyWord + "'");
										break;
									}

									gap = 0;

									exportpp = null;
									String exportname = null;

									while (str.hasMoreTokens())
									{
										keyWord = str.nextToken();
										String temp = keyWord;
										int equalsLoc = keyWord.indexOf("=");
										if (equalsLoc != -1)
										{
											temp = keyWord.substring(0, equalsLoc);										
										}

										if (temp.equals("gap"))
										{
											String temp2;
											if (keyWord.indexOf("=") != -1)
											{
												if (keyWord.substring(keyWord.indexOf("=")+1) == "")
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
												keyWord = str.nextToken();

												if (keyWord.charAt(0) == '=')
												{
													temp2 = keyWord.substring(1);										
												}
												else
												{
													System.out.println("Line " + lineno + ": missing '=' after 'gap'");
													break;
												}
											}

											gap = Integer.parseInt(temp2);
										}
										else if (temp.equals("export"))
										{
											if (!str.hasMoreTokens())
											{
												System.out.println("Line " + lineno + ": missing port name after 'export'");
												break;
											}

											keyWord = str.nextToken();
											String temp2;
											if(keyWord.indexOf("=") != -1)
											{
												temp = keyWord.substring(0, keyWord.indexOf("="));
												if (keyWord.substring(keyWord.indexOf("=")+1) == "")
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
												temp = keyWord;
												keyWord = str.nextToken();

												if (keyWord.charAt(0) == '=')
												{
													temp2 = keyWord.substring(1);										
												}
												else
												{
													System.out.println("Line " + lineno + ": missing '=' after 'export PORT'");
													break;
												}									
											}

											exportpp = np.findPortProto(temp);
											if (exportpp == null)
											{
												System.out.println("Line " + lineno + ": no port '" + temp + "' on cell '" + temp + "'");
												break;
											}

											exportname = temp2;
											if (exportname.equals(""))
											{
												System.out.println("Line " + lineno + ": missing export name after 'export PORT='");
												break;
											}	
										}
										else
										{
											PortAssociate pa = new PortAssociate();
											pa.ni = null;
											pa.pp = np.findPortProto(temp);
											if (pa.pp == null)
											{
												System.out.println("Line " + lineno + ": no export '" + temp + "' in cell " + np.describe());
											}

											String temp2;
											if (keyWord.indexOf("=") != -1)
											{
												if (keyWord.substring(keyWord.indexOf("=")+1) == "")
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
												keyWord = str.nextToken();

												if(keyWord.charAt(0) == '=')
												{
													temp2 = keyWord.substring(1);										
												}
												else
												{
													System.out.println("Line " + lineno + ": missing '=' after pad port name");
													break;
												}
											}

											if (corenp == null)
											{
												System.out.println("Line " + lineno + ": no core cell for association");
												break;
											}
											pa.corepp = corenp.findPortProto(temp2);
											if (pa.corepp == null)
											{
												System.out.println("Line " + lineno + ": no port '" + temp2 + "' on cell '" + temp2 + "'");
												break;
											}
											arrayPortAssociate.add(pa);
										}
									}					
									double centerX = 0;
									double centerY = 0;
									if (lastni != null)
									{
										String cellname = (lastni.getProtoEquivalent()).noLibDescribe();
										ArrayAlign aa = null;
										for(Iterator it = arrayAlignList.iterator(); it.hasNext(); )
										{
											aa = (ArrayAlign)it.next();
											if (aa.cell.equals(cellname)) break;
											aa = null;
										}
										if (aa == null)
										{
											System.out.println("Line " + lineno + ": no port alignment given for cell " + lastni.describe());
											break;
										}

										pp = (lastni.getProto()).findPortProto(aa.outport); 
										if (pp == null)
										{
											System.out.println("Line " + lineno + ": no port called '" + aa.outport + "' on cell" + lastni.describe());
											break;
										}

										Poly poly = (lastni.findPortInstFromProto(pp)).getPoly();
										centerX = poly.getCenterX();
										centerY = poly.getCenterY();
										width = poly.getBounds().getWidth();
										height = poly.getBounds().getHeight();

										cellname = np.whichCell().noLibDescribe();

										aa = null;
										for(Iterator it = arrayAlignList.iterator(); it.hasNext(); )
										{
											aa = (ArrayAlign)it.next();
											if (aa.cell.equals(cellname)) break;
											aa = null;
										}
										if (aa == null)
										{
											System.out.println("Line " + lineno + ": no port called '" + aa.outport + "' on cell " + np.describe());
											break;
										}									
										pp = np.findPortProto(aa.inport);

										if (pp == null)
										{
											System.out.println("Line " + lineno + ": no port called '" + aa.inport + "' on cell " + np.describe());
											break;
										}
									}

									//corneroffset(NONODEINST,np,angle,0,&ox,&oy,false);
									Point2D pointCenter = new Point2D.Double(centerX, centerY);
									NodeInst ni = NodeInst.makeInstance(np, pointCenter, np.getDefWidth(), np.getDefHeight(), angle, cell, null);
									if (ni == null)
									{
										System.out.println("Line " + lineno + ": problem creating" + np.describe() + " instance");
										break;
									}

									if (lastni != null)
									{
										switch (angle)
										{
											case 0:    gapx =  gap;   gapy =    0;   break;
											case 900:  gapx =    0;   gapy =  gap;   break;
											case 1800: gapx = -gap;   gapy =    0;   break;
											case 2700: gapx =    0;   gapy = -gap;   break;
										}
										Poly poly = ni.findPortInstFromProto(pp).getPoly();
										double tempx = centerX - (poly.getCenterX()) - gapx;
										double tempy = centerY - (poly.getCenterY()) - gapy;
										ni.modifyInstance(tempx, tempy, 0, 0, 0);
									}
									if (exportpp != null)
									{
										pp = Export.newInstance(cell, ni.findPortInstFromProto(exportpp), exportname);
									}
									lastni = ni;

									for(Iterator it=arrayPortAssociate.iterator(); it.hasNext(); )
									{
										PortAssociate pa = (PortAssociate)it.next();
										if (pa.ni == null) pa.ni = ni;
									}
									continue;
								}

								System.out.println("Line " + lineno + ": unknown keyword'" + keyWord + "'");
								break;

							} while(str.hasMoreTokens());
						}
					}

					lineRead = readLine.readLine();
					lineno++;
				}
				if (corenp != null)
				{
					Rectangle2D bounds = cell.getBounds();
					Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
					EditWindow.gridAlign(center);

					SizeOffset so = corenp.getSizeOffset();
					NodeInst ni = NodeInst.makeInstance(corenp, center, corenp.getDefWidth(), corenp.getDefHeight(), 0, cell, null);

					for(Iterator it=arrayPortAssociate.iterator(); it.hasNext(); )
					{
						PortAssociate pa = (PortAssociate)it.next();
						if (pa.ni == null) continue;

						PortInst pi1 = ni.findPortInstFromProto(pa.corepp);
						PortInst pi2 = pa.ni.findPortInstFromProto(pa.pp);
						PrimitiveArc ap = Generic.tech.unrouted_arc;
						ArcInst ai = ArcInst.newInstance(ap, ap.getDefaultWidth(), pi1, pi2, null);
					}
				}

				WindowFrame.createEditWindow(cell);

			} catch (IOException e1) {}
		}
	}

	public void ArrayFromFile()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.PADARR, null);
		if (fileName == null)
		{
			System.out.println("File not found");
			return;
		}
		PadFrame padFrame = new PadFrame(fileName);
	}

}

