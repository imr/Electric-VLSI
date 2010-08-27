/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.scala

import com.sun.electric.Main.UserInterfaceDummy
import com.sun.electric.database.CellRevision
import com.sun.electric.database.CellTree
import com.sun.electric.database.ImmutableNodeInst
import com.sun.electric.database.geometry.PolyBase
import com.sun.electric.database.geometry.bool.DeltaMerge
import com.sun.electric.database.geometry.bool.UnloadPolys
import com.sun.electric.database.geometry.bool.VectorCache
import com.sun.electric.database.hierarchy.Cell
import com.sun.electric.database.hierarchy.EDatabase
import com.sun.electric.database.hierarchy.Library
import com.sun.electric.database.id.CellId
import com.sun.electric.database.id.IdManager
import com.sun.electric.database.text.Pref
import com.sun.electric.database.text.TextUtils
import com.sun.electric.database.variable.TextDescriptor
import com.sun.electric.technology.Layer
import com.sun.electric.technology.Technology
import com.sun.electric.tool.Job
import com.sun.electric.tool.Tool
import com.sun.electric.tool.io.FileType
import com.sun.electric.tool.io.input.LibraryFiles

import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import scala.collection.JavaConversions
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.LinkedHashSet

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

  def loadLibrary(libPath: String): Library = {
    val database = EDatabase.serverDatabase
    database.lowLevelBeginChanging(null)
    try {
      val libUrl = TextUtils.makeURLToFile(libPath)
      val libName = TextUtils.getFileNameWithoutExtension(libUrl)
      val fileType = if (libPath.endsWith(".delib")) FileType.DELIB else FileType.JELIB
      LibraryFiles.readLibrary(libUrl, libName, fileType, true)
    } finally {
      database.backup
      database.lowLevelEndChanging
    }
  }
  
  def loadCell(libFile: String, cellName: String): Cell = loadLibrary(libFile).findNodeProto(cellName)

  def countHier(top: CellTree, localCount: CellTree => Int): Int = downTop(top).map(localCount).sum

  def countFlat(top: CellTree, localCount: CellTree => Int): (CellId => Int) = {
    val cells = downTop(top)
    val result = LinkedHashMap[CellId,Int]()
    for (val t: CellTree <- cells) {
      val nodes = JavaConversions.asIterable(t.top.cellRevision.nodes).filter(n => n.protoId.isInstanceOf[CellId])
      val c = localCount(t) + nodes.map(n => result(n.protoId.asInstanceOf[CellId])).sum
      result.put(t.top.cellRevision.d.cellId, c)
    }
    result
  }

  def downTop(top: CellTree): Seq[CellTree] = {
    val result = LinkedHashSet[CellTree]()
    def downTop(t: CellTree): Unit = {
      if (!result.contains(t)) {
        for (subTree <- t.getSubTrees) downTop(subTree)
        result.add(t)
      }
    }
    downTop(top)
    result.toSeq
  }

  def mergeLayer(vectorCache: VectorCache, cellId: CellId, layer: Layer) = {
    val boxCoords = new Array[Int](4)
    val numBoxes = vectorCache.getNumBoxes(cellId, layer)
    val rectV = Vector.tabulate(numBoxes) { i =>
      vectorCache.getBoxes(cellId, layer, i, 1, boxCoords)
      val lx = boxCoords(0)
      val ly = boxCoords(1)
      val hx = boxCoords(2)
      val hy = boxCoords(3)
      new Rectangle(lx, ly, hx - lx, hy - ly)
    }
    val rectL = JavaConversions.asList(rectV)

    val bout = new ByteArrayOutputStream
    val out = new DataOutputStream(bout)
    val dm = new DeltaMerge()
    dm.loop(rectL, out)
    val ba = bout.toByteArray
    out.close

    val inpS = new DataInputStream(new ByteArrayInputStream(ba))
    val up = new UnloadPolys();
    val trees = up.loop(inpS, false);
    inpS.close

    def countPoints(t: PolyBase.PolyBaseTree): Int = {
      val l = t.getSons
      val sonCount = if (l.isEmpty) 0 else {
        JavaConversions.asIterable(l).map(st => countPoints(st)).sum
      }
      t.getPoly.getPoints.length + sonCount
    }

    val mergedPoints = JavaConversions.asIterable(trees).map(countPoints).sum
//    println(cellId + " " + rectV.size*4 + " -> " + mergedPoints)
    mergedPoints
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    initElectric

    val libPath = args(0)
    val topCellName = args(1)
    val topCell = loadCell(libPath, topCellName)
    val dt = downTop(topCell.tree)
    println("downTop " + dt.size)
    for (val t <- dt) {
      println(t)
    }
    val vectorCache = new VectorCache(topCell.getDatabase.backup)
    def countHier(localCount: CellRevision => Int): Int = {
      Main.countHier(topCell.tree, (t: CellTree) => localCount(t.top.cellRevision))
    }
    println(countHier(r => 1) + " cells")
    println(countHier(r => vectorCache.getSubcells(r.d.cellId).size) + " subCells")
    println(countHier(r => r.nodes.size) + " nodes")
    println(countHier(r => r.arcs.size) + " arcs")
    println(countHier(r => r.exports.size) + " exports")

    def countFlat(localCount: CellRevision => Int): Int = {
      Main.countFlat(topCell.tree, (t: CellTree) => localCount(t.top.cellRevision))(topCell.getId)
    }
    println(countFlat(r => 1) + " cell insts")
    println(countFlat(r => vectorCache.getSubcells(r.d.cellId).size) + " subCells")
    println(countFlat(r => r.nodes.size) + " node insts")
    println(countFlat(r => r.arcs.size) + " arc insts")
    println(countFlat(r => r.exports.size) + " export insts")

    vectorCache.scanLayers(topCell.getId)
    val layers = JavaConversions.asIterable(vectorCache.getLayers)
    for (val layer <- layers) {
      println(layer)
      println(countHier(r => vectorCache.getNumBoxes(r.d.cellId, layer)) + " boxes")
      println(countFlat(r => vectorCache.getNumBoxes(r.d.cellId, layer)) + " box insts")
    }
    val m1 = layers.find(l => l.getName == "Metal-1").get
    println(m1)
    println(countHier(r => vectorCache.getNumBoxes(r.d.cellId, m1))*4 + " points")
    println(countFlat(r => vectorCache.getNumBoxes(r.d.cellId, m1))*4 + " box points")
    val mergedCount = new LinkedHashMap[CellId,Int]()
    for (val t <- dt) {
      val cellId = t.top.cellRevision.d.cellId
      val mergedC = mergeLayer(vectorCache, cellId, m1)
      mergedCount.put(cellId, mergedC)
    }
    println(countHier(r => mergedCount(r.d.cellId)) + " merged points")
    println(countFlat(r => mergedCount(r.d.cellId)) + " merged point insts")

    val via1 = layers.find(l => l.getName == "Via-1").get
    println(via1)
    println(countHier(r => vectorCache.getNumBoxes(r.d.cellId, via1))*4 + " points")
    println(countFlat(r => vectorCache.getNumBoxes(r.d.cellId, via1))*4 + " box points")
    val mergedCount2 = new LinkedHashMap[CellId,Int]()
    for (val t <- dt) {
      val cellId = t.top.cellRevision.d.cellId
      val mergedC = mergeLayer(vectorCache, cellId, via1)
      mergedCount2.put(cellId, mergedC)
    }
    println(countHier(r => mergedCount2(r.d.cellId)) + " merged points")
    println(countFlat(r => mergedCount2(r.d.cellId)) + " merged point insts")
  }

}
