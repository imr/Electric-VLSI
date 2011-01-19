/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.plugins.minarea.bitmapscala

import com.sun.electric.api.minarea.LayoutCell
import com.sun.electric.api.minarea.MinAreaChecker

import java.util.Properties

class BitMapMinAreaChecker extends MinAreaChecker {

    override def getAlgorithmName = "BitMap"
  
    override def getDefaultParameters = new Properties()
  
    /**
     * @param topCell top cell of the layout
     * @param minArea minimal area of valid polygon
     * @param parameters algorithm parameters
     * @param errorLogger an API to report violations
     */
    override def check(topCell: LayoutCell, minArea: Long, parameters: Properties, errorLogger: MinAreaChecker.ErrorLogger) = {
      println("topCell " + topCell.getName)
      println("minArea " + minArea)
      println("parameters " + parameters)
    }

}
