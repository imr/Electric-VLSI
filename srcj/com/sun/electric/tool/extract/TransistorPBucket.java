package com.sun.electric.tool.extract;

import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Mar 18, 2005
 * Time: 10:07:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class TransistorPBucket implements ExtractedPBucket
{
    public String gateName;
    public String sourceName;
    public String drainName;
    private TransistorSize size;
    private double sourceArea, drainArea, sourcePerim, drainPerim;
    public NodeInst ni;
    private double mFactor;
    
    public TransistorPBucket(NodeInst ni, TransistorSize size, String gName, String sName, String dName,
                             double factor)
    {
        this.ni = ni;
        this.size = size;
        this.gateName = gName;
        this.sourceName = sName;
        this.drainName = dName;
        this.mFactor = factor;
    }

    public char getType()
    {
        char type = (ni.getFunction() == PrimitiveNode.Function.TRANMOS || ni.getFunction() == PrimitiveNode.Function.TRA4NMOS) ?
                'n' : 'p';
        return type;
    }

    public double getTransistorLength(double legnthOffset)
    {
        return size.getDoubleLength() - legnthOffset;
    }

    public double getTransistorWidth() {return size.getDoubleWidth() * mFactor;}

    public double getActiveArea() {return DBMath.round(getTransistorWidth() * size.getDoubleActiveLength());}

    public double getActivePerim() {return DBMath.round((getTransistorWidth() + size.getDoubleActiveLength())*2);}

    public void addDifussionInformation(String net, double area, double perimeter)
    {
        if (net.equals(drainName))
        {
            drainArea += area;
            drainPerim += perimeter;
        }
        else if (net.equals(sourceName))
        {
            sourceArea += area;
            sourcePerim += perimeter;
        }
        else
            System.out.println("Error in TransistorPBucket.addDifussionInformation");
    }

    public String getInfo(Technology tech)
    {
        // Only valid for IRSIM now
        double scale = tech.getScale();
        double legnthOffset = tech.getGateLengthSubtraction()/scale;
        //scale /= 10;
        double length = getTransistorLength(legnthOffset);
        double width = getTransistorWidth();
//        double activeArea = getActiveArea();
//        double activePerim = getActivePerim();
//                    activeArea = size.getDoubleWidth() * 6;
//            activePerim= size.getDoubleWidth() + 12;

        char type = getType();
        StringBuffer line = new StringBuffer();
        line.append(type);
        line.append(" " + gateName + " " + sourceName + " " + drainName);
        line.append(" " + TextUtils.formatDouble(length));
        line.append(" " + TextUtils.formatDouble(width));
        line.append(" " + TextUtils.formatDouble(ni.getAnchorCenterX()));
        line.append(" " + TextUtils.formatDouble(ni.getAnchorCenterY()));
        if (type == 'n') line.append(" g=S_gnd");
        else line.append(" g=S_vdd"); // type = 'p'
//        line.append(" s=A_" + (int)activeArea + ",P_" + (int)activePerim);
//        line.append(" d=A_" + (int)activeArea + ",P_" + (int)activePerim);
//        double areaScale = ParasiticTool.getAreaScale(scale); // area in square microns
//        double perimScale = ParasiticTool.getPerimScale(scale);           // perim in microns
        line.append(" s=A_" + (int)(sourceArea) + ",P_" + (int)(sourcePerim));
        line.append(" d=A_" + (int)(drainArea) + ",P_" + (int)(drainPerim));
        return line.toString();
    }
}
