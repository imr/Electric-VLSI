package com.sun.electric.tool.extract;

import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.PrimitiveNode;
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

    public double getTransistorLength() {return size.getDoubleLength();}

    public double getTransistorWidth() {return size.getDoubleWidth() * mFactor;}

    public double getActiveArea() {return DBMath.round(getTransistorWidth() * size.getDoubleActiveLength());}

    public double getActivePerim() {return DBMath.round((getTransistorWidth() + size.getDoubleActiveLength())*2);}

    public String getInfo(double scale)
    {
        // Only valid for IRSIM now
        double length = getTransistorLength();
        double width = getTransistorWidth();
        double activeArea = getActiveArea();
        double activePerim = getActivePerim();
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
        line.append(" s=A_" + (int)activeArea + ",P_" + (int)activePerim);
        line.append(" d=A_" + (int)activeArea + ",P_" + (int)activePerim);
        return line.toString();
    }
}
