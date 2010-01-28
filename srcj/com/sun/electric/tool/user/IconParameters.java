package com.sun.electric.tool.user;

import com.sun.electric.tool.Job;
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

import java.io.Serializable;
import java.util.*;
import java.awt.geom.Point2D;

/**
	 * Class to define parameters for automatic icon generation
 */
public class IconParameters implements Serializable
{
    /** length of leads from body to export */							double leadLength;
    /** spacing between leads (or exports) */							double leadSpacing;
    /** true to place exports by location in original cell */			boolean placeByCellLocation;
    /** true to place exports exactly by location in original cell */   boolean useExactLocation;
    /** true to reverse placement of exports */							boolean reverseIconExportOrder;
    /** true to draw an icon body (a rectangle) */						boolean drawBody;
    /** size (in units) of text on body */								double bodyTextSize;
    /** true to draw leads between the body and exports */				boolean drawLeads;
    /** true to place a cell-center in the icon */						boolean placeCellCenter;
    /** technology: 0=generic, 1=schematic */							int exportTech;
    /** text style: 0=centered, 1=inward, 2=outward */					int exportStyle;
    /** text location: 0=on body, 1=end of lead, 2=middle of lead */	int exportLocation;
    /** true to make exports "always drawn" */							boolean alwaysDrawn;
    /** side for input ports (when placeByCellLocation false) */		int inputSide;
    /** side for output ports (when placeByCellLocation false) */		int outputSide;
    /** side for bidir ports (when placeByCellLocation false) */		int bidirSide;
    /** side for power ports (when placeByCellLocation false) */		int pwrSide;
    /** side for ground ports (when placeByCellLocation false) */		int gndSide;
    /** side for clock ports (when placeByCellLocation false) */		int clkSide;
    /** rotation of input text (when placeByCellLocation false) */		int inputRot;
    /** rotation of output text (when placeByCellLocation false) */		int outputRot;
    /** rotation of bidir text (when placeByCellLocation false) */		int bidirRot;
    /** rotation of power text (when placeByCellLocation false) */		int pwrRot;
    /** rotation of ground text (when placeByCellLocation false) */		int gndRot;
    /** rotation of clock text (when placeByCellLocation false) */		int clkRot;
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
        boolean drawBodyAndLeads = Job.getDebug();
        leadLength = 2.0;
        leadSpacing = 2.0;
        placeByCellLocation = false;
        useExactLocation = false;
        reverseIconExportOrder = false;
        drawBody = drawBodyAndLeads;
        bodyTextSize = 1.0;
        drawLeads = drawBodyAndLeads;
        placeCellCenter = true;
        exportTech = 0;
        exportStyle = 1;
        exportLocation = 1;
        alwaysDrawn = false;
        inputSide = 0;
        outputSide = 1;
        bidirSide = 2;
        pwrSide = 3;
        gndSide = 3;
        clkSide = 0;
        inputRot = 0;
        outputRot = 0;
        bidirRot = 0;
        pwrRot = 0;
        gndRot = 0;
        clkRot = 0;
        topRot = 0;
        bottomRot = 0;
        leftRot = 0;
        rightRot = 0;
if (userDefaults) initFromUserDefaults();
}

    public void initFromUserDefaults()
    {
        leadLength = User.getIconGenLeadLength();
        leadSpacing = User.getIconGenLeadSpacing();
        placeByCellLocation = User.getIconGenExportPlacement() == 1;
        useExactLocation = User.getIconGenExportPlacementExact();
        reverseIconExportOrder = User.isIconGenReverseExportOrder();
        drawBody = User.isIconGenDrawBody();
        bodyTextSize = User.getIconGenBodyTextSize();
        drawLeads = User.isIconGenDrawLeads();
        placeCellCenter = User.isPlaceCellCenter();
        exportTech = User.getIconGenExportTech();
        exportStyle = User.getIconGenExportStyle();
        exportLocation = User.getIconGenExportLocation();
        alwaysDrawn = User.isIconsAlwaysDrawn();
        inputSide = User.getIconGenInputSide();
        outputSide = User.getIconGenOutputSide();
        bidirSide = User.getIconGenBidirSide();
        pwrSide = User.getIconGenPowerSide();
        gndSide = User.getIconGenGroundSide();
        clkSide = User.getIconGenClockSide();
        inputRot = User.getIconGenInputRot();
        outputRot = User.getIconGenOutputRot();
        bidirRot = User.getIconGenBidirRot();
        pwrRot = User.getIconGenPowerRot();
        gndRot = User.getIconGenGroundRot();
        clkRot = User.getIconGenClockRot();
        topRot = User.getIconGenTopRot();
        bottomRot = User.getIconGenBottomRot();
        leftRot = User.getIconGenLeftRot();
        rightRot = User.getIconGenRightRot();
    }

    public double getIconGenLeadLength() { return leadLength; }
    public boolean isIconGenDrawLeads() { return drawLeads; }
    public int getIconGenExportTech() { return exportTech; }
    public int getIconGenExportStyle() { return exportStyle; }
    public int getIconGenExportLocation() { return exportLocation; }
    public boolean isIconsAlwaysDrawn() { return alwaysDrawn; }
    int getIconGenInputRot() { return inputRot; }
    int getIconGenOutputRot() { return outputRot; }
    int getIconGenBidirRot() { return bidirRot; }
    int getIconGenPowerRot() { return pwrRot; }
    int getIconGenGroundRot() { return gndRot; }
    int getIconGenClockRot() { return clkRot; }

/**
     * Method to create an icon for a cell.
     * @param curCell the cell to turn into an icon.
     * @return the icon cell (null on error).
     */
    public Cell makeIconForCell(Cell curCell)
        throws JobException
{
        // create the new icon cell
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
        if (placeByCellLocation)
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
            if (reverseIconExportOrder)
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
                int rotation = ViewChanges.iconTextRotation(pp, this);
                portRotation.put(pp, new Integer(rotation));
            }
        }

        // determine the size of the "black box" core
        double xSize, ySize;
        if (placeByCellLocation && useExactLocation)
        {
            xSize = curCell.getDefWidth();
            ySize = curCell.getDefHeight();
        } else
        {
            ySize = Math.max(Math.max(leftSide, rightSide), 5) * leadSpacing;
            xSize = Math.max(Math.max(topSide, bottomSide), 3) * leadSpacing;
        }

        // create the "black box"
        NodeInst bbNi = null;
        if (drawBody)
        {
            bbNi = NodeInst.newInstance(Artwork.tech().openedThickerPolygonNode, new Point2D.Double(0,0), xSize, ySize, iconCell);
            if (bbNi == null) return null;
            EPoint[] boxOutline = new EPoint[5];
            if (placeByCellLocation && useExactLocation)
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
            TextDescriptor td = TextDescriptor.getAnnotationTextDescriptor().withRelSize(bodyTextSize);
            bbNi.newVar(Schematics.SCHEM_FUNCTION, curCell.getName(), td);
        }

        // place pins around the Black Box
        int total = 0;
        for(Export pp : exportList)
        {
            // determine location and side of the port
            int portPosition = portIndex.get(pp).intValue();
            int index = portSide.get(pp).intValue();
            double spacing = leadSpacing;
            double xPos = 0, yPos = 0;
            double xBBPos = 0, yBBPos = 0;
            if (placeByCellLocation && useExactLocation)
            {
                xBBPos = xPos = pp.getOriginalPort().getCenter().getX();
                yBBPos = yPos = pp.getOriginalPort().getCenter().getY();
            } else
            {
                switch (index)
                {
                    case 0:		// left side
                        xBBPos = -xSize/2;
                        xPos = xBBPos - leadLength;
                        if (leftSide*2 < rightSide) spacing = leadSpacing * 2;
                        yBBPos = yPos = ySize/2 - ((ySize - (leftSide-1)*spacing) / 2 + portPosition * spacing);
                        break;
                    case 1:		// right side
                        xBBPos = xSize/2;
                        xPos = xBBPos + leadLength;
                        if (rightSide*2 < leftSide) spacing = leadSpacing * 2;
                        yBBPos = yPos = ySize/2 - ((ySize - (rightSide-1)*spacing) / 2 + portPosition * spacing);
                        break;
                    case 2:		// top
                        if (topSide*2 < bottomSide) spacing = leadSpacing * 2;
                        xBBPos = xPos = xSize/2 - ((xSize - (topSide-1)*spacing) / 2 + portPosition * spacing);
                        yBBPos = ySize/2;
                        yPos = yBBPos + leadLength;
                        break;
                    case 3:		// bottom
                        if (bottomSide*2 < topSide) spacing = leadSpacing * 2;
                        xBBPos = xPos = xSize/2 - ((xSize - (bottomSide-1)*spacing) / 2 + portPosition * spacing);
                        yBBPos = -ySize/2;
                        yPos = yBBPos - leadLength;
                        break;
                }
            }

            int rotation = portRotation.get(pp).intValue();
            if (makeIconExport(pp, index, xPos, yPos, xBBPos, yBBPos, iconCell, rotation, this))
                    total++;
        }

        // if no body, leads, or cell center is drawn, and there is only 1 export, add more
        if (!drawBody && !drawLeads && placeCellCenter && total <= 1)
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
     * @param exportTech the technology to use (generic or schematic)
     * @param drawLeads true to draw leads on the icon
     * @param exportStyle the icon style
     * @param exportLocation
     * @param textRotation
     * @param alwaysDrawn true to make export text be "always drawn"
     * @return true if the export was created.
     */
    public static boolean makeIconExport(Export pp, int index, double xPos, double yPos, double xBBPos, double yBBPos,
                                         Cell np, int textRotation, IconParameters iconParameters)
    {
        // presume "universal" exports (Generic technology)
        NodeProto pinType = Generic.tech().universalPinNode;
        double pinSizeX = 0, pinSizeY = 0;
        if (iconParameters.exportTech != 0)
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
        if (!iconParameters.drawLeads)
        {
            xPos = xBBPos;   yPos = yBBPos;
        }

        // make the pin with the port
        NodeInst pinNi = NodeInst.newInstance(pinType, new Point2D.Double(xPos, yPos), pinSizeX, pinSizeY, np);
        if (pinNi == null) return false;

        // export the port that should be on this pin
        PortInst pi = pinNi.getOnlyPortInst();
        Export port = Export.newInstance(np, pi, pp.getName(), pp.getCharacteristic(), iconParameters);
        if (port != null)
        {
            TextDescriptor td = port.getTextDescriptor(Export.EXPORT_NAME);
            if (textRotation != 0) td = td.withRotation(TextDescriptor.Rotation.getRotationAt(textRotation));
            switch (iconParameters.exportStyle)
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
            int loc = iconParameters.exportLocation;
            if (!iconParameters.drawLeads) loc = 0;
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
            port.setAlwaysDrawn(iconParameters.alwaysDrawn);
            port.copyVarsFrom(pp);
        }

        // add lead if requested
        if (iconParameters.drawLeads)
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
