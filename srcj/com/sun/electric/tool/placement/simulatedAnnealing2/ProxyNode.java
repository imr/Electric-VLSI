/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProxyNode.java
 * Written by Team 6: Sebastian Roether, Jochen Lutz
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
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
package com.sun.electric.tool.placement.simulatedAnnealing2;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is a proxy for the actual placement node.
 * It can be moved around, rotated and cloned without touching the placement node
 */
final class ProxyNode implements Comparable<ProxyNode>
{
	private double x;
	private double y;
	private Orientation orientation = null;
	public double width = 0;					// rotated width of the placement node
	public double height = 0;					// rotated height of the placement node
	public boolean finalized = false;
	
	private PlacementNode node = null;

	private ArrayList<PlacementNetwork> nets = new ArrayList<PlacementNetwork>();
	private HashMap<PlacementNetwork, PlacementNetwork> netMap = null;
	
	/**
	 * This class is a proxy for <PlacementPort>.
	 * It calculates its position relative to a <ProxyNode> not to a <PlacementNode>
	 * 
	 * @author Basti
	 *
	 */
	private final class ProxyPort extends PlacementPort
	{
		private double rotatedOffX, rotatedOffY;
		private ProxyNode proxyNode;
		private double offX;
		private double offY;
		

		/**
		 * Constructor to create a PlacementPort.
		 * @param x the X offset of this PlacementPort from the center of its PlacementNode.
		 * @param y the Y offset of this PlacementPort from the center of its PlacementNode.
		 * @param pp the Electric PortProto of this PlacementPort.
		 */
		public ProxyPort(double x, double y, ProxyNode p)
		{
			super(x, y, null);
			offX = x;
			offY = y;
			this.proxyNode = p;
			setPlacementNode(p.getNode());
		}

		/**
		 * Returns rotated coordinates
		 */
		public double getRotatedOffX() { return rotatedOffX; }
		public double getRotatedOffY() { return rotatedOffY; }

		/**
		 * Internal method to compute the rotated offset of this PlacementPort
		 * assuming that the Orientation of its ProxyNode has changed.
		 */
		public void computeRotatedOffset()
		{
			Orientation orient = proxyNode.getPlacementOrientation();
			if (orient == Orientation.IDENT)
			{
				rotatedOffX = offX;
				rotatedOffY = offY;
				return;
			}
			AffineTransform trans = orient.pureRotate();
			Point2D offset = new Point2D.Double(offX, offY);
			trans.transform(offset, offset);
			rotatedOffX = offset.getX();
			rotatedOffY = offset.getY();
		}
	}
	
	
	/**
	 * Constructor to create a ProxyNode
	 * @param node the PlacementNode that should be proxied
	 * @param ignoredNets a list of nets that should be ignored
	 */
	public ProxyNode(PlacementNode node, ArrayList<PlacementNetwork> ignoredNets)
	{
		this.node = node;
		this.x = node.getPlacementX();
		this.y = node.getPlacementY();
		this.width = node.getWidth();
		this.height = node.getHeight();

		// create a list of all nets that node belongs to
		for(PlacementPort p : node.getPorts())
			if(!nets.contains(p.getPlacementNetwork()) && p.getPlacementNetwork() != null)
				if(!ignoredNets.contains(p.getPlacementNetwork()))
					nets.add(p.getPlacementNetwork());
		
		orientation = node.getPlacementOrientation();
	}
	
	/**
	 * Constructor used by cloning that doesn't create a new net list
	 * @param node
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param o orientation of the cloned node
	 * @param nets the netlist of the cloned node
	 */
	private ProxyNode(PlacementNode node, double x, double y, double width, double height, Orientation o, ArrayList<PlacementNetwork> nets)
	{
		this.node = node;
		this.x = x;
		this.y = y;
		this.nets = nets;
		this.width = width;
		this.height = height;
		this.orientation = o;
	}
	
	/**
	 * Method that applies the changes made to the proxy to the placement node
	 */
	public void apply()
	{
		node.setPlacement(x, y);
		node.setOrientation(orientation);
	}
		
	/**
	 * Method that sets the node to a new position
	 * @param x
	 * @param y
	 */
	public void setPlacement(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Method that returns the proxied PlacementNode
	 * @return the PlacementNode
	 */
	public PlacementNode getNode() {
		return this.node;
	}
	
	/**
	 * Method that clones this proxy
	 */
	public ProxyNode clone() {
		return new ProxyNode(this.node, x, y, width, height, orientation, nets);
	}
	
	/**
	 * Method to get a list of nets this node belongs to.
	 * This is more convenient than iterating over the ports
	 * Also it only contains nets that are not ignored (e.g. because
	 * they are too huge)
	 * @return the list of nets this node belongs to
	 */
	public ArrayList<PlacementNetwork> getNets() {
		return nets;
	}
		
	/**
	 * Method to get the X-dimension of the location of this ProxyNode
	 * @return
	 */
	public double getPlacementX() {
		return this.x;
	}
		
	/**
	 * Method to get the Y-dimension of the location of this ProxyNode
	 * @return
	 */
	public double getPlacementY() {
		return this.y;
	}
	
	/**
	 * Method to get the orientation of this ProxyNode
	 * @return
	 */
	public Orientation getPlacementOrientation() {
		return orientation;
	}
	
	/**
	 * Method that returns the net, that was "cloned" when rotating this port.
	 * When evaluating a rotation perturbation, the cloned proxy creates flat
	 * copys of all nets it belongs to an replaces its ports in all the nets
	 * with proxy ports that calculate their offset relative to the now rotated
	 * proxy node (instead of the not rotated placement node)
	 * 
	 * It is intended to be called only on a clone of a ProxyNode when getting
	 * the correct hashValue to save the calculated net metric.
	 * @param alteredNet
	 * @return
	 */
	public PlacementNetwork getOriginalNet(PlacementNetwork alteredNet)
	{
		if(netMap == null) return alteredNet;
		return netMap.get(alteredNet);
	}
	
	/**
	 * Method that sets the orientation of the proxy node
	 * @param o the new orientation
	 * @param forDummy sets whether the proxy node replaces ports in its nets
	 */
	public void setPlacementOrientation(Orientation o, boolean forDummy) {
		
		Orientation oldOrientation = orientation;
		orientation = o;
		
		if(forDummy)
		{
			netMap = new HashMap<PlacementNetwork, PlacementNetwork>();
			ArrayList<PlacementNetwork> newNets = new ArrayList<PlacementNetwork>(nets.size());
			
			/*
			 * Replace the list of nets with a new list. Replace every
			 * port on this node with a proxy port an calculate their offsets
			 * relative to this node
			 */
			for(PlacementNetwork net : nets)
			{
				ArrayList<PlacementPort> newPortsOnNet = new ArrayList<PlacementPort>();

				// Replace ports that belong to this node with proxies
				for(PlacementPort port : net.getPortsOnNet())
				{
					if(port.getPlacementNode() == this.node)
					{
						// get the unrotated offsets (they are private in the framework...)
						// by applying the inverse rotation to the rotated values
						AffineTransform trans = oldOrientation.inverse().pureRotate();
						Point2D offset = new Point2D.Double(port.getRotatedOffX(), port.getRotatedOffY());
						trans.transform(offset, offset);
						ProxyPort pp = new ProxyPort(offset.getX(),offset.getY(), this);
						
						// calculate the port rotated offsets relative to this proxy node !
						pp.computeRotatedOffset();
						newPortsOnNet.add(pp);
					}
					else
						newPortsOnNet.add(port);
				}
				
				PlacementNetwork newNet = new PlacementNetwork(newPortsOnNet);
				netMap.put(net, newNet);
				newNets.add(newNet);
			}
				
			this.nets = newNets;	
		}
		else
		{
			// this actually makes the rotation visible to other threads!
			node.setOrientation(o);
		}	
		
		// swap height and width for 90°, 270°, ...
		if (o == Orientation.R || o == Orientation.RRR || o == Orientation.XR || o == Orientation.XRRR || o == Orientation.XYR || o == Orientation.XYRRR || o == Orientation.YR || o == Orientation.YRRR)
		{
			width = node.getHeight();
			height = node.getWidth();
		}	
		else
		{
			width = node.getWidth();
			height = node.getHeight();
		}
	}

	public int compareTo(ProxyNode o) {
		double dist1 = x * x + y * y;
		double dist2 = o.x * o.x + o.y * o.y;
		
		if(dist1 > dist2)
			return 1;
		else
			return -1;
	}
}
