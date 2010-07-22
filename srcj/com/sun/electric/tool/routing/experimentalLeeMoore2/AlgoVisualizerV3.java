/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AlgoVisualizerV3.java
 * Written by: Alexander Herzog (Team 4)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalLeeMoore2;

import java.awt.*;
import javax.swing.*;

import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.RegionBorder;

import java.awt.geom.*;
import java.awt.geom.Rectangle2D.Double;

class AlgoVisualizerV3 extends JFrame implements Runnable// create frame for canvas
{
	GCanvas canvas;
	int seg_id = 0;
  public AlgoVisualizerV3(GlobalRouterV3 router) // constructor
  {
    super("AlgoVisualizer");
    
    setBounds(50,50,1000,800);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Container con=this.getContentPane();
    con.setBackground(Color.white);
    canvas=new GCanvas(router);
    con.add(canvas);
  }
  
  synchronized public void RepaintVis(int seg_id, Vector2i pos){
		  canvas.cur_pos = pos;
		  canvas.seg_id = seg_id;
		  canvas.repaint();
	  
  }
  
  public void run(){
    setVisible(true);
	  
  }
}

class GCanvas extends Canvas // create a canvas for your graphics
{
	GlobalRouterV3 router;
	double scale = 1.0d / 2.0d;
	public int seg_id;
	Vector2i cur_pos = new Vector2i(-1, -1);
	
	public GCanvas(GlobalRouterV3 router){
		this.router = router;
	}
	
  public void paint(Graphics g) // display shapes on canvas
  {
    Graphics2D g2D=(Graphics2D) g; // cast to 2D
    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON);
    
    for(int x = 0; x < router.regions_x; ++x){
    	for(int y = 0; y < router.regions_y; ++y){
    		Rectangle2D.Double rect = new Rectangle2D.Double();
    		rect.setFrameFromDiagonal(x * router.region_width * scale, y 
    				* router.region_height * scale, (x + 1) * router.region_width * scale, 
    				(y + 1) * router.region_height * scale);
    		
    		g2D.draw(rect);
    		    		
    		BacktraceState min = router.RegionAt(x, y).segment_infos[seg_id].GetMin(Integer.MAX_VALUE);
    		if(min == null){
        		continue;
    		}
    		
    		float was_visited = router.RegionAt(x, y).segment_infos[seg_id].was_part_of_bt ? 0.0f : 0.5f;
    		was_visited = router.RegionAt(x, y).segment_infos[seg_id].is_initialized ? was_visited : 0.0f;
    		
//    		int supply_left = -1;
//    		int demand_left = -1;
//    		int demand_up= -1;
//    		int supply_up = -1;
//    		RegionBorder border = router.RegionAt(x, y).GetRegionBorder(RegionDirection.rd_left);
//    		if(border != null){
//    			supply_left = border.GetSupply();
//    			demand_left = border.GetDemand();
//    		}
//    		border = router.RegionAt(x, y).GetRegionBorder(RegionDirection.rd_up);
//    		if(border != null){
//    			supply_up = border.GetSupply();
//    			demand_up = border.GetDemand();
//    		}
			
    		String sdir = "";
    		switch(min.dir){
    		case rd_down:
    			sdir = "D";
    			break;
    		case rd_up:
    			sdir = "U";
    			break;
    		case rd_left:
    			sdir = "L";
    			break;
    		case rd_right:
    			sdir = "R";
    			break;
    		case rd_undefined:
    			sdir = "nDef";
    			break;
    		}
    		
    		float cheap_path_factor = (float)min.path_length / 15;
    		cheap_path_factor = cheap_path_factor > 1.0f ? 1.0f : cheap_path_factor;

    		Color c = g2D.getColor();
    		g2D.setColor(new Color(was_visited, 0.0f, cheap_path_factor));
    		g2D.fill(rect);
    		g2D.setColor(c); 
    		g2D.drawString(sdir, (int)(x * router.region_width * scale), (int)(y * router.region_height * scale + g2D.getFont().getSize()));
//    		String supl = Integer.toString(supply_left);
//    		String supu = Integer.toString(supply_up);
//    		String deml = Integer.toString(demand_left);
//    		String demu = Integer.toString(demand_up);

//    		g2D.drawString(deml, (int)(x * router.region_width * scale), (int)(y * router.region_height * scale + router.region_height * scale * 0.5));
//    		g2D.drawString(demu, (int)(x * router.region_width * scale + router.region_width * scale * 0.5), (int)(y * router.region_height * scale + router.region_height * scale));
    	}
    }
    
	Rectangle2D.Double r = new Rectangle2D.Double();
	r.setFrameFromDiagonal(cur_pos.x * router.region_width * scale, cur_pos.y 
			* router.region_height * scale, (cur_pos.x + 1) * router.region_width * scale, 
			(cur_pos.y + 1) * router.region_height * scale);
	
	Color tmp_c = g2D.getColor();
	g2D.setColor(Color.BLUE);
	g2D.draw(r);
	g2D.setColor(tmp_c);
	
  }
  public void drawArc(Graphics2D g2D,int x1,int y1,int x2,int y2,int sd,int rd,int cl){
	  Arc2D.Float arc1=new Arc2D.Float(x1,y1,x2,y2,sd,rd,cl);
	  g2D.fill(arc1);
	  }
  public void drawEllipse(Graphics2D g2D,int x1,int y1,int x2,int y2){
	  Ellipse2D.Float oval1=new Ellipse2D.Float(x1,y1,x2,y2);
	  g2D.fill(oval1);
	  }
}