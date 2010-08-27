/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.scala

import com.sun.electric.Main.UserInterfaceDummy
import com.sun.electric.database.geometry.bool.VectorCache
import com.sun.electric.database.hierarchy.EDatabase
import com.sun.electric.database.hierarchy.Library
import com.sun.electric.database.id.IdManager
import com.sun.electric.database.text.Pref
import com.sun.electric.database.text.TextUtils
import com.sun.electric.database.variable.TextDescriptor
import com.sun.electric.technology.Technology
import com.sun.electric.tool.Job
import com.sun.electric.tool.Tool
import com.sun.electric.tool.io.FileType
import com.sun.electric.tool.io.input.LibraryFiles

object Main {

  def initElectric = {
    TextDescriptor.cacheSize
    Tool.initAllTools
    Pref.lockCreation

    val database = new EDatabase(IdManager.stdIdManager.getInitialSnapshot(), "serverDB")
    Job.setUserInterface(new UserInterfaceDummy() {
	override def getDatabase = EDatabase.serverDatabase
        override def getCurrentCell = null
      });
    EDatabase.setServerDatabase(database)
    database.lock(true)
    Technology.initPreinstalledTechnologies(database, Technology.getParamValuesByXmlPath())
  }

  def loadLibrary(libName: String, fileName: String): Library = {
    val database = EDatabase.serverDatabase
    database.lowLevelBeginChanging(null)
    try {
      val fileURL = TextUtils.makeURLToFile(fileName)
      val fileType = if (fileName.endsWith(".delib")) FileType.DELIB else FileType.JELIB
      LibraryFiles.readLibrary(fileURL, libName, fileType, true)
    } finally {
      database.backup
      database.lowLevelEndChanging
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    initElectric

    val libPath = args(0)
    val topCellName = args(1)
    val libUrl = TextUtils.makeURLToFile(libPath)
    val libName = TextUtils.getFileNameWithoutExtension(libUrl)
    val lib = loadLibrary(libName, libPath)
    println(Library.getNumLibraries + " libs")
    val topCell = lib.findNodeProto(topCellName)
    val vectorCache = new VectorCache(EDatabase.serverDatabase.backup)
    vectorCache.scanLayers(topCell.getId)
    for (val layer <- List(vectorCache.getLayers)) {
      println(layer)
    }
  }

}
