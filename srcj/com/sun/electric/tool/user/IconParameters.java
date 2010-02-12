package com.sun.electric.tool.user;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.tool.JobException;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.TechPool;

import java.io.Serializable;
import java.util.*;
import java.awt.geom.Point2D;

/**
	 * Class to define parameters for automatic icon generation
 */
public class IconParameters implements Serializable
{
    /** side for input ports (when placeByCellLocation false) */		int inputSide;
    /** side for output ports (when placeByCellLocation false) */		int outputSide;
    /** side for bidir ports (when placeByCellLocation false) */		int bidirSide;
    /** side for power ports (when placeByCellLocation false) */		int pwrSide;
    /** side for ground ports (when placeByCellLocation false) */		int gndSide;
    /** side for clock ports (when placeByCellLocation false) */		int clkSide;
    /** rotation of top text (when placeByCellLocation true) */			int topRot;
    /** rotation of bottom text (when placeByCellLocation true) */		int bottomRot;
    /** rotation of left text (when placeByCellLocation true) */		int leftRot;
    /** rotation of right text (when placeByCellLocation true) */		int rightRot;

    public static IconParameters makeInstance(boolean userDefaults)
    {
        return new IconParameters(userDefaults);
    }

    private IconParameters(boolean userDefaults)
    {
        inputSide = 0;
        outputSide = 1;
        bidirSide = 2;
        pwrSide = 3;
        gndSide = 3;
        clkSide = 0;
        topRot = 0;
        bottomRot = 0;
        leftRot = 0;
        rightRot = 0;
if (userDefaults) initFromUserDefaults();
}

    public void initFromUserDefaults()
    {
        inputSide = User.getIconGenInputSide();
        outputSide = User.getIconGenOutputSide();
        bidirSide = User.getIconGenBidirSide();
        pwrSide = User.getIconGenPowerSide();
        gndSide = User.getIconGenGroundSide();
        clkSide = User.getIconGenClockSide();
        topRot = User.getIconGenTopRot();
        bottomRot = User.getIconGenBottomRot();
        leftRot = User.getIconGenLeftRot();
        rightRot = User.getIconGenRightRot();
    }

    /**
     * Method to create an icon for a cell.
     * @param curCell the cell to turn into an icon.
     * @return the icon cell (null on error).
     */
    public Cell makeIconForCell(Cell curCell)
        throws JobException
{
        // create the new icon cell
        EditingPreferences ep = curCell.getEditingPreferences();
        String iconCellName = curCell.getName() + "{ic}";
        Cell iconCell = Cell.makeInstance(curCell.getLibrary(), iconCellName);
        if (iconCell == null)
            throw new JobException("Cannot create Icon cell " + iconCellName);
        iconCell.setWantExpanded();

        // determine number of ports on each side
        int leftSide = 0, rightSide = 0, bottomSide = 0, topSide = 0;
        Map<Export,Integer> portIndex = new HashMap<Export,Integer>();
        Map<Export,Integer> portSide = new HashMap<Export,Integer>();
        Map<Export,Integer> portRotation = new HashMap<Export,Integer>();

        // make a sorted list of exports
        List<Export> exportList = new ArrayList<Export>();
        for(Iterator<PortProto> it = curCell.getPorts(); it.hasNext(); )
        {
            Export pp = (Export)it.next();
            if (pp.isBodyOnly()) continue;
            exportList.add(pp);
        }
        if (ep.iconGenExportPlacement == 1)
        {
            // place exports according to their location in the cell
            Collections.sort(exportList, new ExportsByAngle());

            // figure out how many exports go on each side
            int numExports = exportList.size();
            leftSide = rightSide = topSide = bottomSide = numExports / 4;
            if (leftSide + rightSide + topSide + bottomSide < numExports) leftSide++;
            if (leftSide + rightSide + topSide + bottomSide < numExports) rightSide++;
            if (leftSide + rightSide + topSide + bottomSide < numExports) topSide++;

            // cache the location of each export
            Map<Export, Point2D> portCenters = new HashMap<Export,Point2D>();
            for(int i=0; i<numExports; i++)
            {
                Export pp = exportList.get(i);
                portCenters.put(pp, pp.getOriginalPort().getCenter());
            }

            // make an array of points in the middle of each side
            ERectangle bounds = curCell.getBounds();
            Point2D leftPoint = new Point2D.Double(bounds.getCenterX() - bounds.getWidth(), bounds.getCenterY());
            Point2D rightPoint = new Point2D.Double(bounds.getCenterX() + bounds.getWidth(), bounds.getCenterY());
            Point2D topPoint = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY() + bounds.getWidth());
            Point2D bottomPoint = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY() - bounds.getWidth());
            Point2D[] sidePoints = new Point2D[numExports];
            int fill = 0;
            for(int i=0; i<leftSide; i++) sidePoints[fill++] = leftPoint;
            for(int i=0; i<topSide; i++) sidePoints[fill++] = topPoint;
            for(int i=0; i<rightSide; i++) sidePoints[fill++] = rightPoint;
            for(int i=0; i<bottomSide; i++) sidePoints[fill++] = bottomPoint;

            // rotate the points and find the rotation with the least distance to the side points
            double [] totDist = new double[numExports];
            for(int i=0; i<numExports; i++)
            {
                totDist[i] = 0;
                for(int j=0; j<numExports; j++)
                {
                    Point2D ppCtr = portCenters.get(exportList.get((j+i)%numExports));
                    double dist = ppCtr.distance(sidePoints[j]);
                    totDist[i] += dist;
                }
            }
            double bestDist = Double.MAX_VALUE;
            int bestIndex = -1;
            for(int i=0; i<numExports; i++)
            {
                if (totDist[i] < bestDist)
                {
                    bestDist = totDist[i];
                    bestIndex = i;
                }
            }

            // assign ports along each side
            for(int i=0; i<leftSide; i++)
            {
                Export pp = exportList.get((i+bestIndex)%numExports);
                portSide.put(pp, new Integer(0));
                portIndex.put(pp, new Integer(leftSide-i-1));
                portRotation.put(pp, new Integer(leftRot));
            }
            for(int i=0; i<topSide; i++)
            {
                Export pp = exportList.get((i+leftSide+bestIndex)%numExports);
                portSide.put(pp, new Integer(2));
                portIndex.put(pp, new Integer(topSide-i-1));
                portRotation.put(pp, new Integer(topRot));
            }
            for(int i=0; i<rightSide; i++)
            {
                Export pp = exportList.get((i+leftSide+topSide+bestIndex)%numExports);
                portSide.put(pp, new Integer(1));
                portIndex.put(pp, new Integer(i));
                portRotation.put(pp, new Integer(rightRot));
            }
            for(int i=0; i<bottomSide; i++)
            {
                Export pp = exportList.get((i+leftSide+topSide+rightSide+bestIndex)%numExports);
                portSide.put(pp, new Integer(3));
                portIndex.put(pp, new Integer(i));
                portRotation.put(pp, new Integer(bottomRot));
            }
        } else
        {
            // place exports according to their characteristics
            if (ep.iconGenReverseExportOrder)
                Collections.reverse(exportList);
            for(Export pp : exportList)
            {
                int index = iconPosition(pp);
                portSide.put(pp, new Integer(index));
                switch (index)
                {
                    case 0: portIndex.put(pp, new Integer(leftSide++));    break;
                    case 1: portIndex.put(pp, new Integer(rightSide++));   break;
                    case 2: portIndex.put(pp, new Integer(topSide++));     break;
                    case 3: portIndex.put(pp, new Integer(bottomSide++));  break;
                }
                int rotation = ViewChanges.iconTextRotation(pp);
                portRotation.put(pp, new Integer(rotation));
            }
        }

        // determine the size of the "black box" core
        double xSize, ySize;
        if (ep.iconGenExportPlacement == 1 && ep.iconGenExportPlacementExact)
        {
            xSize = curCell.getDefWidth();
            ySize = curCell.getDefHeight();
        } else
        {
            ySize = Math.max(Math.max(leftSide, rightSide), 5) * ep.iconGenLeadSpacing;
            xSize = Math.max(Math.max(topSide, bottomSide), 3) * ep.iconGenLeadSpacing;
        }

        // create the "black box"
        NodeInst bbNi = null;
        if (ep.iconGenDrawBody)
        {
            bbNi = NodeInst.newInstance(Artwork.tech().openedThickerPolygonNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
            if (bbNi == null) return null;
            EPoint[] boxOutline = new EPoint[5];
            if (ep.iconGenExportPlacement == 1 && ep.iconGenExportPlacementExact)
            {
                boxOutline[0] = new EPoint(curCell.getBounds().getMinX(), curCell.getBounds().getMinY());
                boxOutline[1] = new EPoint(curCell.getBounds().getMinX(), curCell.getBounds().getMaxY());
                boxOutline[2] = new EPoint(curCell.getBounds().getMaxX(), curCell.getBounds().getMaxY());
                boxOutline[3] = new EPoint(curCell.getBounds().getMaxX(), curCell.getBounds().getMinY());
                boxOutline[4] = new EPoint(curCell.getBounds().getMinX(), curCell.getBounds().getMinY());
            } else
            {
                boxOutline[0] = new EPoint(-xSize/2, -ySize/2);
                boxOutline[1] = new EPoint(-xSize/2,  ySize/2);
                boxOutline[2] = new EPoint( xSize/2,  ySize/2);
                boxOutline[3] = new EPoint( xSize/2, -ySize/2);
                boxOutline[4] = new EPoint(-xSize/2, -ySize/2);
            }
            bbNi.setTrace(boxOutline);

            // put the original cell name on it
            TextDescriptor td = TextDescriptor.getAnnotationTextDescriptor().withRelSize(ep.iconGenBodyTextSize);
            bbNi.newVar(Schematics.SCHEM_FUNCTION, curCell.getName(), td);
        }

        // place pins around the Black Box
        int total = 0;
        for(Export pp : exportList)
        {
            // determine location and side of the port
            int portPosition = portIndex.get(pp).intValue();
            int index = portSide.get(pp).intValue();
            double spacing = ep.iconGenLeadSpacing;
            double xPos = 0, yPos = 0;
            double xBBPos = 0, yBBPos = 0;
            if (ep.iconGenExportPlacement == 1 && ep.iconGenExportPlacementExact)
            {
                xBBPos = xPos = pp.getOriginalPort().getCenter().getX();
                yBBPos = yPos = pp.getOriginalPort().getCenter().getY();
            } else
            {
                switch (index)
                {
                    case 0:		// left side
                        xBBPos = -xSize/2;
                        xPos = xBBPos - ep.iconGenLeadLength;
                        if (leftSide*2 < rightSide) spacing = ep.iconGenLeadSpacing * 2;
                        yBBPos = yPos = ySize/2 - ((ySize - (leftSide-1)*spacing) / 2 + portPosition * spacing);
                        break;
                    case 1:		// right side
                        xBBPos = xSize/2;
                        xPos = xBBPos + ep.iconGenLeadLength;
                        if (rightSide*2 < leftSide) spacing = ep.iconGenLeadSpacing * 2;
                        yBBPos = yPos = ySize/2 - ((ySize - (rightSide-1)*spacing) / 2 + portPosition * spacing);
                        break;
                    case 2:		// top
                        if (topSide*2 < bottomSide) spacing = ep.iconGenLeadSpacing * 2;
                        xBBPos = xPos = xSize/2 - ((xSize - (topSide-1)*spacing) / 2 + portPosition * spacing);
                        yBBPos = ySize/2;
                        yPos = yBBPos + ep.iconGenLeadLength;
                        break;
                    case 3:		// bottom
                        if (bottomSide*2 < topSide) spacing = ep.iconGenLeadSpacing * 2;
                        xBBPos = xPos = xSize/2 - ((xSize - (bottomSide-1)*spacing) / 2 + portPosition * spacing);
                        yBBPos = -ySize/2;
                        yPos = yBBPos - ep.iconGenLeadLength;
                        break;
                }
            }

            int rotation = portRotation.get(pp).intValue();
            if (makeIconExport(pp, index, xPos, yPos, xBBPos, yBBPos, iconCell, rotation))
                    total++;
        }

        // if no body, leads, or cell center is drawn, and there is only 1 export, add more
        if (!ep.iconGenDrawBody && !ep.iconGenDrawLeads && ep.placeCellCenter && total <= 1)
        {
            NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
        }

        return iconCell;
    }

    /**
     * Method to determine the side of the icon that port "pp" belongs on.
     */
    private int iconPosition(Export pp)
    {
        PortCharacteristic character = pp.getCharacteristic();

        // special detection for power and ground ports
        if (pp.isPower()) character = PortCharacteristic.PWR;
        if (pp.isGround()) character = PortCharacteristic.GND;

        // see which side this type of port sits on
        if (character == PortCharacteristic.IN) return inputSide;
        if (character == PortCharacteristic.OUT) return outputSide;
        if (character == PortCharacteristic.BIDIR) return bidirSide;
        if (character == PortCharacteristic.PWR) return pwrSide;
        if (character == PortCharacteristic.GND) return gndSide;
        if (character.isClock()) return clkSide;
        return inputSide;
    }

    /**
	 * Helper method to create an export in an icon.
     * @param pp the Export to build.
     * @param index the side (0: left, 1: right, 2: top, 3: bottom).
     * @param xPos the export location
     * @param yPos the export location
     * @param xBBPos the central box location
     * @param yBBPos the central box location.
     * @param np the cell in which to create the export.
     * @param textRotation
     * @return true if the export was created.
     */
    public static boolean makeIconExport(Export pp, int index, double xPos, double yPos, double xBBPos, double yBBPos,
                                         Cell np, int textRotation)
    {
        EditingPreferences ep = pp.getEditingPreferences();
        // presume "universal" exports (Generic technology)
        NodeProto pinType = Generic.tech().universalPinNode;
        double pinSizeX = 0, pinSizeY = 0;
        if (ep.iconGenExportTech != 0)
        {
            // instead, use "schematic" exports (Schematic Bus Pins)
            pinType = Schematics.tech().busPinNode;
            pinSizeX = pinType.getDefWidth();
            pinSizeY = pinType.getDefHeight();
        }

        // determine the type of wires used for leads
        ArcProto wireType = Schematics.tech().wire_arc;
        if (pp.getBasePort().connectsTo(Schematics.tech().bus_arc) && pp.getNameKey().isBus())
        {
            wireType = Schematics.tech().bus_arc;
            pinType = Schematics.tech().busPinNode;
            pinSizeX = pinType.getDefWidth();
            pinSizeY = pinType.getDefHeight();
        }

        // if the export is on the body (no leads) then move it in
        if (!ep.iconGenDrawLeads)
        {
            xPos = xBBPos;   yPos = yBBPos;
        }

        // make the pin with the port
        NodeInst pinNi = NodeInst.newInstance(pinType, new Point2D.Double(xPos, yPos), pinSizeX, pinSizeY, np);
        if (pinNi == null) return false;

        // export the port that should be on this pin
        PortInst pi = pinNi.getOnlyPortInst();
        Export port = Export.newInstance(np, pi, pp.getName(), pp.getCharacteristic(), null);
        if (port != null)
        {
            TextDescriptor td = port.getTextDescriptor(Export.EXPORT_NAME);
            if (textRotation != 0) td = td.withRotation(TextDescriptor.Rotation.getRotationAt(textRotation));
            switch (ep.iconGenExportStyle)
            {
                case 0:		// Centered
                    td = td.withPos(TextDescriptor.Position.CENT);
                    break;
                case 1:		// Inward
                    switch (index)
                    {
                        case 0: td = td.withPos(TextDescriptor.Position.RIGHT);  break;	// left
                        case 1: td = td.withPos(TextDescriptor.Position.LEFT);   break;	// right
                        case 2: td = td.withPos(TextDescriptor.Position.DOWN);   break;	// top
                        case 3: td = td.withPos(TextDescriptor.Position.UP);     break;	// bottom
                    }
                    break;
                case 2:		// Outward
                    switch (index)
                    {
                        case 0: td = td.withPos(TextDescriptor.Position.LEFT);   break;	// left
                        case 1: td = td.withPos(TextDescriptor.Position.RIGHT);  break;	// right
                        case 2: td = td.withPos(TextDescriptor.Position.UP);     break;	// top
                        case 3: td= td.withPos(TextDescriptor.Position.DOWN);   break;	// bottom
                    }
                    break;
            }
            port.setTextDescriptor(Export.EXPORT_NAME, td);
            double xOffset = 0, yOffset = 0;
            int loc = ep.iconGenExportLocation;
            if (!ep.iconGenDrawLeads) loc = 0;
            switch (loc)
            {
                case 0:		// port on body
                    xOffset = xBBPos - xPos;   yOffset = yBBPos - yPos;
                    break;
                case 1:		// port on lead end
                    break;
                case 2:		// port on lead middle
                    xOffset = (xPos+xBBPos) / 2 - xPos;
                    yOffset = (yPos+yBBPos) / 2 - yPos;
                    break;
            }
            port.setOff(Export.EXPORT_NAME, xOffset, yOffset);
            port.setAlwaysDrawn(ep.iconsAlwaysDrawn);
            port.copyVarsFrom(pp);
        }

        // add lead if requested
        if (ep.iconGenDrawLeads)
        {
            pinType = wireType.findPinProto();
            if (pinType == Schematics.tech().busPinNode)
                pinType = Generic.tech().invisiblePinNode;
            double wid = pinType.getDefWidth();
            double hei = pinType.getDefHeight();
            NodeInst ni = NodeInst.newInstance(pinType, new Point2D.Double(xBBPos, yBBPos), wid, hei, np);
            if (ni != null)
            {
                PortInst head = ni.getOnlyPortInst();
                PortInst tail = pinNi.getOnlyPortInst();
                ArcInst ai = ArcInst.makeInstance(wireType,
                    head, tail, new Point2D.Double(xBBPos, yBBPos),
                        new Point2D.Double(xPos, yPos), null);
                if (ai != null && wireType == Schematics.tech().bus_arc)
                {
                    ai.setHeadExtended(false);
                    ai.setTailExtended(false);
                }
            }
        }
        return true;
    }

    /**
	 * Comparator class for sorting Exports by their angle about the cell center.
	 */
	private static class ExportsByAngle implements Comparator<Export>
	{
		/**
		 * Method to sort Exports by their angle about the cell center.
		 */
		public int compare(Export p1, Export p2)
		{
			Cell cell = p1.getParent();
			ERectangle bounds = cell.getBounds();
			Point2D cellCtr = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
			Point2D p1Ctr = p1.getOriginalPort().getCenter();
			Point2D p2Ctr = p2.getOriginalPort().getCenter();
			double angle1 = DBMath.figureAngleRadians(cellCtr, p1Ctr);
			double angle2 = DBMath.figureAngleRadians(cellCtr, p2Ctr);
			if (angle1 < angle2) return 1;
			if (angle1 > angle2) return -1;
			return 0;
		}
	}
}
