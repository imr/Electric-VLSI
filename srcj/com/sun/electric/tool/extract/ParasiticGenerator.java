package com.sun.electric.tool.extract;

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.network.Netlist;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Mar 17, 2005
 * Time: 10:20:06 AM
 * To change this template use File | Settings | File Templates.
 */
/**
 * This class should be used to create ExtractedPBucket
 * depending on tool
 */
public interface ParasiticGenerator {
    public ExtractedPBucket createBucket(NodeInst ni, ParasiticTool.ParasiticCellInfo info);
}
