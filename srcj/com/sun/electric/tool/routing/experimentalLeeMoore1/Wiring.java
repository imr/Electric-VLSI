/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Wiring.java
 * Written by: Andreas Uebelhoer, Alexander Bieles, Emre Selegin (Team 6)
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
package com.sun.electric.tool.routing.experimentalLeeMoore1;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import com.sun.electric.tool.routing.RoutingFrame.RoutePoint;
import com.sun.electric.tool.routing.RoutingFrame.RouteWire;
import com.sun.electric.tool.routing.RoutingFrame.RoutingContact;
import com.sun.electric.tool.routing.RoutingFrame.RoutingLayer;
import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;
import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.Tupel;

/**
 * This class offers functions to create wires in electric out of created routes and between tupels.
 */
public class Wiring {

    private static boolean DEBUG = false;
    protected static final boolean REPORTING = true;

    private static HashMap<String, Integer> metalLayerMap;
	private static List<RoutingLayer> allLayers;
	private static List<RoutingContact> allContacts;

	public static void init(List<RoutingLayer> layers, HashMap<String, Integer> map, List<RoutingContact> allContacts, boolean output) {
		DEBUG = output;
	    metalLayerMap = map;
	    allLayers = layers;
	    Wiring.allContacts = allContacts;
	}

	/**
     * Connect two tupels. ATTENTION: they have to be on the same layer!
     * This method is only suited for connecting connectionPoints.
     * @param rs RoutingSegment where wire should be created
     * @param tA first tupel
     * @param tB second tupel
     * @return RoutePoints created at the tupels' positions
     */
    public static RoutePoint[] connect(RoutingSegment rs, Tupel tA, Tupel tB) {
        RoutingLayer layer = getRoutingLayerByTupelLayer(tA.getLayer());
        RoutePoint r1 = placePin(tA.getLayer(), tA);
        RoutePoint r2 = placePin(tB.getLayer(), tB);
        connectRoutePoints(rs, r1, r2, layer);
        RoutePoint[] result = {r1, r2};
        return result;
    }
    
    /**
     * Connect two route points.
     * @param rs routing segment where wire is created 
     * @param rp1 first route point
     * @param rp2 second route point
     */
    public static void connect(RoutingSegment rs, RoutePoint rp1, RoutePoint rp2){
    	connectRoutePoints(rs, rp1, rp2, rp1.getContact().getFirstLayer());
    }
    
    /**
     * Place route points at the location given by the two tupels.
     * @param tA first tupel
     * @param tB second tupel
     * @return
     */
    public static RoutePoint[] getRoutePoints(Tupel tA, Tupel tB) {
        RoutePoint r1 = placePin(tA.getLayer(), tA);
        RoutePoint r2 = placePin(tB.getLayer(), tB);
        RoutePoint[] result = {r1, r2};
        return result;
    }

    /**
     * Connects two route points of a routing segment lying on the same layer. This method is synchronized!
     * @param rs RoutingSegment
     * @param r1 first RoutePoint
     * @param r2 second RoutePoint
     * @param layer RoutingLayer
     */
    synchronized private static void connectRoutePoints(RoutingSegment rs, RoutePoint r1, RoutePoint r2, RoutingLayer layer) {
        if (DEBUG) {
            System.out.println("WIRING: Verbinde " + r1.getLocation() + "->" + r2.getLocation());
        }
        rs.addWireEnd(r1);
        rs.addWireEnd(r2);
        RouteWire rw = new RouteWire(layer, r1, r2, layer.getMinWidth());
        rs.addWire(rw);
    }
    
    /**
     * Calls addWire and connects two routing points on the same layer.
     * @param rs RoutingSegment where wire should be created
     * @param r1 first RoutePoint
     * @param r2 second RoutePoint
     * @param layer on which wire is created
     */
    private static void createWire(RoutingSegment rs, RoutePoint r1, RoutePoint r2, RoutingLayer layer) {
        if (DEBUG) {
            System.out.println("createWire@WIRING: Verbinde " + r1.getLocation() + "->" + r2.getLocation()+" auf Layer "+layer.getName());
        }
        RouteWire rw = new RouteWire(layer, r1, r2, layer.getMinWidth());
        rs.addWire(rw);
    }
    
    //	public static void connect(RoutingPart rp,List<Tupel> edgePoints) {
//		RoutingSegment rs=rp.rs;
//		Point2D start=rs.getStartEnd().getLocation();
//		Point2D end=rs.getFinishEnd().getLocation();
//		RoutePoint rp1;
//		int i=0;
//		if(containsStart(rs,edgePoints)){
//			rp1=new RoutePoint(RoutingContact.STARTPOINT, start, 0);
//			rs.addWireEnd(rp1);
//			
//		}else{
//			Tupel t1=edgePoints.get(0);
//			RoutingLayer l=getRoutingLayerByTupelLayer(t1.getLayer());
//			rp1=new RoutePoint(l.getPin(),new Point2D.Double(t1.getX_InsideElectric(), t1.getY_InsideElectric()),0);
//            //rs.addWireEnd(rp1);//TODO: rein?
//			if (REPORTING) SimpleQualityReporter.addPin();
//			i++;//TODO: weg?
//		}
//		
//		for (;i<edgePoints.size()-1;i++) {
//			Tupel t1=edgePoints.get(i);
//			Tupel t2=edgePoints.get(i+1);
//			//routing points are on same position but different layers so we need a via here
//			if(t1.isEqualPosition(t2) && t1.getLayer()!=t2.getLayer()){
//				RoutingLayer rl1 = getRoutingLayerByTupelLayer(t1.getLayer());
//				RoutingLayer rl2 = getRoutingLayerByTupelLayer(t2.getLayer());
//				RoutePoint via=placeVia(rl1,rl2,t1.getX_InsideElectric(),t1.getY_InsideElectric());
//				if (REPORTING) SimpleQualityReporter.addVia();
//				connectRoutePoints(rs, rp1, via, rl1);
//				rp1=via;
//			}	
//		}
//		Tupel last=edgePoints.get(edgePoints.size()-1);
//		//last routing point was NOT a via
//		//create a new routing point located at the location of the last tupel
//		//connect it with last via and make it new last routing point
//		if(!(last.isEqualPosition(rp1))){
//			RoutingLayer layer=getRoutingLayerByTupelLayer(last.getLayer());
//			RoutePoint rp2=new RoutePoint(layer.getPin(),new Point2D.Double(last.getX_InsideElectric(),last.getY_InsideElectric()),0);
//			if (REPORTING) SimpleQualityReporter.addPin();
//			connectRoutePoints(rs, rp1, rp2, layer);
//			rp1=rp2;
//
//		}
//		//add a wire between last routing point and finish point
//		if(containsEnd(rs, edgePoints)){
//			RoutePoint rp2=new RoutePoint(RoutingContact.FINISHPOINT, end, 0);
//			rs.addWireEnd(rp2);
//			connectRoutePoints(rs, rp1, rp2, rs.getFinishLayers().get(0));	
//		}
//		
//	}
    
    /**
     * Create wires between a list of tupels that symbolize the route of the given routing part.
     * @param rp RoutingPart
     * @param edgePoints list of tupels that build the route
     */
    public static void connect(RoutingPart rp, List<Tupel> edgePoints) {
        boolean makeAngularWiresRectangular = true;
        
        if(DEBUG && rp.rs.getFinishEnd().getLocation().equals(new Point2D.Double(1929.2, 331.6))){
        	System.out.println("Have it...");
        }
        
        LinkedList<WiringPart> contacts = new LinkedList<WiringPart>();
        boolean lastContactWasVia = false;
        RoutePoint rp1 = rp.getStartRoutePoint();
        boolean firstPoint = true;
        if(containsStart(rp.rs, edgePoints)){
        	firstPoint=false;
        }
        
        Tupel t1;
        Tupel t2;
        int i=0;
        t1 = edgePoints.get(i);
        t2 = edgePoints.get(i + 1);
        //first point is rounded start point
        if (makeAngularWiresRectangular && containsStart(rp.rs, edgePoints)) {
        	boolean penultimateWireDirection = getWireDirection(t1, t2);
            RoutePoint rp2=null;
            if (penultimateWireDirection == WorkerThread.X_DIRECTION) {
                //we have to correct the second point of the routingpart so, that is at the same x-coordinate
                rp2=placeContact(rp.rs.getStartLayers().get(0), rp1.getLocation().getX(),t1.getY_InsideElectric());
                
            }
            if (penultimateWireDirection == WorkerThread.Y_DIRECTION) {
                //we have to replace the last point so, that it has the same y-coordinate
            	rp2=placeContact(rp.rs.getStartLayers().get(0),t1.getX_InsideElectric(), rp1.getLocation().getY());
            }
            if(rp2==null && DEBUG) System.out.println("RP2 ist null");
            contacts.add(new WiringPart(rp.rs.getStartLayers().get(0), rp1, rp2, firstPoint, false));
            firstPoint=true;
            rp1=rp2;
        }
        
        for (i=1; i < edgePoints.size() - 1; i++) {
            t1 = edgePoints.get(i);
            t2 = edgePoints.get(i + 1);
            if (t1.isEqualPosition(t1) && (t1.getLayer() != t2.getLayer())) {
                //this is a via
                RoutePoint rp2 = placeVia(t1.getLayer(), t2.getLayer(), t1.getX_InsideElectric(), t1.getY_InsideElectric());
                contacts.add(new WiringPart(getRoutingLayerByTupelLayer(t1.getLayer()), rp1, rp2, firstPoint, false));
                lastContactWasVia = true;
                rp1 = rp2;
            } else {
                if (!lastContactWasVia) {
                    RoutePoint rp2 = placePin(t1.getLayer(), t1);
                    contacts.add(new WiringPart(getRoutingLayerByTupelLayer(t1.getLayer()), rp1, rp2, firstPoint, false));
                    lastContactWasVia = false;
                    rp1 = rp2;
                }
            }
            firstPoint = true;
        }
        
        i=edgePoints.size()-1;
        t1 = edgePoints.get(i);
        t2 = edgePoints.get(i - 1);
        //last point is rounded end point
        if (makeAngularWiresRectangular && containsEnd(rp.rs, edgePoints)) {
        	boolean penultimateWireDirection = getWireDirection(t1, t2);
            RoutePoint rp2=null;
            if (penultimateWireDirection == WorkerThread.X_DIRECTION) {
                //we have to correct the second point of the routingpart so, that is at the same x-coordinate
                rp2=placeContact(rp.rs.getFinishLayers().get(0), rp.getEndRoutePoint().getLocation().getX(),t1.getY_InsideElectric());
                
            }
            if (penultimateWireDirection == WorkerThread.Y_DIRECTION) {
                //we have to replace the last point so, that it has the same y-coordinate
            	rp2=placeContact(rp.rs.getFinishLayers().get(0),t1.getX_InsideElectric(), rp.getEndRoutePoint().getLocation().getY());
            }
            if(rp2==null && DEBUG) System.out.println("RP2 ist null");
            contacts.add(new WiringPart(rp.rs.getFinishLayers().get(0), rp1, rp2, firstPoint, false));
            //firstPoint=true;
            rp1=rp2;
        }
        
        t2=edgePoints.get(edgePoints.size()-1);
        RoutePoint rp2 = rp.getEndRoutePoint();
        boolean lastPoint=true;
        if(containsEnd(rp.rs, edgePoints)){
        	lastPoint=false;
        }
        contacts.add(new WiringPart(getRoutingLayerByTupelLayer(t2.getLayer()), rp1, rp2, firstPoint, lastPoint));
        wireRoutePoints(contacts, rp.rs);
    }

    /**
     * Create wire ends and wires for the routes.
     * @param routes list of routes to wire
     * @param rs RoutingSegment the wires are on
     */
    private static void wireRoutePoints(List<WiringPart> routes, RoutingSegment rs) {
        for (WiringPart wiringPart : routes) {
            RoutePoint rp1 = wiringPart.getStart();
            RoutePoint rp2 = wiringPart.getEnd();
            if (!wiringPart.isAlreadyWired_start) {
                rs.addWireEnd(rp1);
            }
            if (!wiringPart.isAlreadyWired_end) {
                rs.addWireEnd(rp2);
            }
            createWire(rs, rp1, rp2, wiringPart.getLayer());
        }
    }

//    private static RoutingLayer getCommonRoutingLayer(RoutePoint rp1, RoutePoint rp2, RoutingSegment rs) {
//        RoutingLayer rp1_fl = rp1.getContact().getFirstLayer();
//        RoutingLayer rp1_sl = rp1.getContact().getSecondLayer();
//        RoutingLayer rp2_fl = rp2.getContact().getFirstLayer();
//        RoutingLayer rp2_sl = rp2.getContact().getSecondLayer();
//        if (rp1_fl == null) {
//            rp1_fl = rp1_sl = rs.getStartLayers().get(0);
//        }
//        if (rp2_fl == null) {
//            rp2_fl = rp2_sl = rs.getFinishLayers().get(0);
//        }
//
//        if (rp1_fl.equals(rp2_fl)) {
//            return rp1_fl;
//        } else if (rp1_fl.equals(rp2_sl)) {
//            return rp1_fl;
//        } else if (rp1_sl.equals(rp2_fl)) {
//            return rp1_sl;
//        } else if (rp1_sl.equals(rp2_sl)) {
//            return rp1_sl;
//        }
//        return null;
//    }

    /**
     * Return whether one of the tupels is the start point of the routing segment.
     * @param rs RoutingSegment
     * @param edgePoints List of tupels
     * @return edgePoints contains start of rs 
     */
    private static boolean containsStart(RoutingSegment rs, List<Tupel> edgePoints) {
        Tupel t1 = edgePoints.get(0);
        Tupel t2 = new Tupel(rs.getStartEnd().getLocation(), 0);
        if (t1.isEqualPosition(t2)) {
            return true;
        }
        return false;
    }
    
//    private static boolean containsStartReversed(RoutingSegment rs, List<Tupel> edgePoints) {
//        Tupel t1 = edgePoints.get(edgePoints.size() - 1);
//        Tupel t2 = new Tupel(rs.getStartEnd().getLocation(), 0);
//        if (t1.isEqualPosition(t2)) {
//            return true;
//        }
//        return false;
//    }

    /**
     * Return whether one of the tupels is the end point of the routing segment.
     * @param rs RoutingSegment
     * @param edgePoints List of tupels
     * @return edgePoints contains end of rs 
     */
    private static boolean containsEnd(RoutingSegment rs, List<Tupel> edgePoints) {
        Tupel t1 = edgePoints.get(edgePoints.size() - 1);
        Tupel t2 = new Tupel(rs.getFinishEnd().getLocation(), 0);
        if (t1.isEqualPosition(t2)) {
            return true;
        }
        return false;
    }
    
    /**
     * this method is used for multiterminal routing (not yet functional)
     * @param rs
     * @param edgePoints
     * @return
     */
    private static boolean containsEndReversed(RoutingSegment rs, List<Tupel> edgePoints) {
        Tupel t1 = edgePoints.get(0);
        Tupel t2 = new Tupel(rs.getFinishEnd().getLocation(), 0);
        if (t1.isEqualPosition(t2)) {
            return true;
        }
        return false;
    }

    /**
     * Place a pin at the location of the tupel.
     * @param layerID Layer on which pin is placed
     * @param t tupel that represents the location
     * @return RoutePoint at the location of the pin
     */
    private static RoutePoint placePin(int layerID, Tupel t) {
        RoutingLayer layer = getRoutingLayerByTupelLayer(layerID);
        
        if (DEBUG){
        	System.out.println("WIRING: Erstelle Pin auf [" + t.getX_InsideElectric() + ", " + t.getY_InsideElectric() + "]");
        }
        return new RoutePoint(layer.getPin(), new Point2D.Double(t.getX_InsideElectric(), t.getY_InsideElectric()), 0);
    }
    
    /**
     * Place a pin at the given location 
     * @param layer Layer on which pin is placed
     * @param x x coordinate
     * @param y y coordinate
     * @return RoutePoint at the location of the pin
     */
    private static RoutePoint placeContact(RoutingLayer layer,double x, double y){
    	if (DEBUG){
        	System.out.println("WIRING: Erstelle Pin auf [" + x + ", " + y + "]");
        }
        return new RoutePoint(layer.getPin(), new Point2D.Double(x, y), 0);
    }

    /**
     * Place a via at the given location
     * @param layer1 first layer connected to the via
     * @param layer2 second layer connected to the via
     * @param x x coordinate
     * @param y y coordinate
     * @return RoutePoint at the location of the via
     */
    private static RoutePoint placeVia(int layer1, int layer2, int x, int y) {
        RoutingLayer startLayer = getRoutingLayerByTupelLayer(layer1);
        RoutingLayer finishLayer = getRoutingLayerByTupelLayer(layer2);

        if (DEBUG) {
            System.out.println("WIRING: Erstelle Via auf [" + x + ", " + y + "];"+startLayer.getName()+"<->"+finishLayer.getName());
        }
        RoutingContact viaContact = getVia(startLayer, finishLayer);
        if (viaContact == null && DEBUG) {
            System.out.println("ERROR: via contact is null. Startlayer: " + startLayer.getMetalNumber() + ", Finishlayer: " + finishLayer.getMetalNumber());
        }
        return new RoutePoint(viaContact, new Point2D.Double(x, y), 0);
    }

    /**
     * Calculate via RoutingContact that connects two layers.
     * @param l1 first layer
     * @param l2 second layer
     * @return RoutingContact via or null if impossible (the layers are not neighboured)
     */
    private static RoutingContact getVia(RoutingLayer l1, RoutingLayer l2) {
        for (RoutingContact rc : allContacts) {
            if ((rc.getFirstLayer().equals(l1) && rc.getSecondLayer().equals(l2)) || (rc.getFirstLayer().equals(l2) && rc.getSecondLayer().equals(l1))) {
                return rc;
            }
        }
        return null;
    }

    /**
     * Get RoutingLayer object by id saved with a tupel
     * @param layer Layer id
     * @return RoutingLayer object
     */
    private static RoutingLayer getRoutingLayerByTupelLayer(int layer) {
        return allLayers.get(metalLayerMap.get("Metal-" + (layer + 1)));
    }

    /**
     * Return whether tupel t2 can be reached from tupel t1 by changing the x coordinate or the y coorinate
     * @see WorkerThread.X_DIRECTION
     * @param t1 first tupel
     * @param t2 second tupel
     * @return direction
     */
    private static boolean getWireDirection(Tupel t1, Tupel t2) {
        if (t1.getX_InsideRoutingArray() == t2.getX_InsideRoutingArray()) {
            return WorkerThread.Y_DIRECTION;
        } else {
            return WorkerThread.X_DIRECTION;
        }
    }

    /**
     * this method is used for multiterminal routing (not yet functional)
     * for now it will always call connect
     * @param rp
     * @param edgePoints
     * @param reversed
     */
	public static void connect(RoutingPart rp, List<Tupel> edgePoints,
			boolean reversed) {
		if(reversed){
			//algorithm for multiterminal wiring
			connectMultiterminal(rp,edgePoints);
		}else{
			//standard algorithm
			connect(rp,edgePoints);
		}
		
	}

	/**
	 * this method is used for multiterminal routing (not yet functional)
	 * @param rp
	 * @param edgePoints
	 */
	private static void connectMultiterminal(RoutingPart rp,
			List<Tupel> edgePoints) {
		boolean makeAngularWiresRectengular = true;
		
		LinkedList<WiringPart> contacts = new LinkedList<WiringPart>();
        boolean lastContactWasVia = false;
        RoutePoint rp1 = rp.getEndRoutePoint();
        boolean firstPoint = true;
        if(containsEndReversed(rp.rs, edgePoints)){
        	firstPoint=false;
        }
		
        Tupel t1;
        Tupel t2;
        int i=0;
        t1 = edgePoints.get(i);
        t2 = edgePoints.get(i + 1);
        //first point is rounded end point
        if (makeAngularWiresRectengular && containsEndReversed(rp.rs, edgePoints)) {
        	boolean penultimateWireDirection = getWireDirection(t1, t2);
            RoutePoint rp2=null;
            if (penultimateWireDirection == WorkerThread.X_DIRECTION) {
                //we have to correct the second point of the routingpart so, that is at the same x-coordinate
                rp2=placeContact(rp.rs.getFinishLayers().get(0), rp1.getLocation().getX(),t1.getY_InsideElectric());
                
            }
            if (penultimateWireDirection == WorkerThread.Y_DIRECTION) {
                //we have to replace the last point so, that it has the same y-coordinate
            	rp2=placeContact(rp.rs.getFinishLayers().get(0),t1.getX_InsideElectric(), rp1.getLocation().getY());
            }
            if(DEBUG)
            if(rp2==null) System.out.println("RP2 ist null");
            contacts.add(new WiringPart(rp.rs.getFinishLayers().get(0), rp1, rp2, firstPoint, false));
            firstPoint=true;
            rp1=rp2;
        }
        
        for (i=1; i < edgePoints.size()-1; i++) {
            t1 = edgePoints.get(i);
            t2 = edgePoints.get(i + 1);
            if (t1.isEqualPosition(t1) && (t1.getLayer() != t2.getLayer())) {
                //this is a via
                RoutePoint rp2 = placeVia(t1.getLayer(), t2.getLayer(), t1.getX_InsideElectric(), t1.getY_InsideElectric());
                contacts.add(new WiringPart(getRoutingLayerByTupelLayer(t1.getLayer()), rp1, rp2, firstPoint, false));
                lastContactWasVia = true;
                rp1 = rp2;
            } else {
                if (!lastContactWasVia) {
                    RoutePoint rp2 = placePin(t1.getLayer(), t1);
                    contacts.add(new WiringPart(getRoutingLayerByTupelLayer(t1.getLayer()), rp1, rp2, firstPoint, false));
                    lastContactWasVia = false;
                    rp1 = rp2;
                }
            }
            firstPoint = true;
        }
        
        t2=edgePoints.get(edgePoints.size()-1);
        RoutePoint rp2 = placePin(t2.getLayer(), t2);
        boolean lastPoint=false;
        contacts.add(new WiringPart(getRoutingLayerByTupelLayer(t2.getLayer()), rp1, rp2, firstPoint, lastPoint));
        
        wireRoutePoints(contacts, rp.rs);
        
	}
}

/**
 * This class represents a part of a wire lying on one layer.
 * @author Andy
 *
 */
class WiringPart {

    RoutingLayer layer;
    RoutePoint start, end;
    boolean isAlreadyWired_start, isAlreadyWired_end;	//remember whether addWireEnd has already been called for this RoutePoint

    public WiringPart(RoutingLayer layer, RoutePoint start, RoutePoint end,
            boolean isAlreadyWiredStart, boolean isAlreadyWiredEnd) {
        this.layer = layer;
        this.start = start;
        this.end = end;
        isAlreadyWired_start = isAlreadyWiredStart;
        isAlreadyWired_end = isAlreadyWiredEnd;
    }

    /**
     * Get layer of this WiringPart
     * @return layer
     */
    public RoutingLayer getLayer() {
        return layer;
    }

    /**
     * Get start of this WiringPart
     * @return start
     */
    public RoutePoint getStart() {
        return start;
    }

    /**
     * Get end of this WiringPart
     * @return end
     */
    public RoutePoint getEnd() {
        return end;
    }

    /**
     * Determine whether starting end has already been wired
     * @return
     */
    public boolean isAlreadyWired_start() {
        return isAlreadyWired_start;
    }

    /**
     * Determine whether ending end has already been wired
     * @return
     */
    public boolean isAlreadyWired_end() {
        return isAlreadyWired_end;
    }
    
    public String toString(){
    	return start.getLocation().toString()+"->"+end.getLocation().toString();
    }
}
