/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Main.scala
 * Written by Dmitry Nadezhin, Sun Microsystems.
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
package com.sun.electric.scala

import com.sun.electric.Main.UserInterfaceDummy
import com.sun.electric.database.CellRevision
import com.sun.electric.database.CellTree
import com.sun.electric.database.geometry.PolyBase
import com.sun.electric.database.geometry.bool.UnloadPolys
import com.sun.electric.database.hierarchy.Cell
import com.sun.electric.database.hierarchy.EDatabase
import com.sun.electric.database.hierarchy.Library
import com.sun.electric.database.id.CellId
import com.sun.electric.database.id.IdManager
import com.sun.electric.database.text.Pref
import com.sun.electric.database.variable.TextDescriptor
import com.sun.electric.technology.Technology
import com.sun.electric.tool.Job
import com.sun.electric.tool.Tool
import com.sun.electric.tool.io.FileType
import com.sun.electric.tool.io.input.LibraryFiles
import com.sun.electric.util.TextUtils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

  def byteArray2tree(ba: Array[Byte]): Iterable[PolyBase.PolyBaseTree] = {
    val inpS = new DataInputStream(new ByteArrayInputStream(ba))
    val up = new UnloadPolys();
    val trees = up.loop(inpS, false)
    inpS.close
    JavaConversions.asIterable(trees)
  }

  def treesSize(ts: Iterable[PolyBase.PolyBaseTree], localCount: PolyBase => Int): Int = {
    def treeSize(t: PolyBase.PolyBaseTree): Int = {
      val l = t.getSons
      val sonCount = treesSize(JavaConversions.asIterable(l), localCount)
      localCount(t.getPoly) + sonCount
    }
    ts.map(t => treeSize(t)).sum
  }

  def hugeFile = {
    val file = File.createTempFile("Electric", "DRC", new File("."))
    file.deleteOnExit
    val out = new FileOutputStream(file)
    val b = new Array[Byte](1 << 20)
    for (val i <- 0 until 8000) out.write(b)
    out.close
    file.delete
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
//    hugeFile
    initElectric

    val libPath = args(0)
    val topCellName = args(1)
    val topCell = loadCell(libPath, topCellName)
    val layoutMerger = new LayoutMergerScalaImpl(topCell)

    val dt = downTop(topCell.tree)
    assert(dt == layoutMerger.downTop(topCell.tree))
    println("downTop " + dt.size)
    for (val t <- dt) {
      println(t)
    }
    val vectorCache = layoutMerger.vectorCache
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
      println
      println(layer)
      println(countHier(r => vectorCache.getNumBoxes(r.d.cellId, layer)*4) + " points")
      println(countFlat(r => vectorCache.getNumBoxes(r.d.cellId, layer)*4) + " point insts")
      val mergedTrees = new LinkedHashMap[CellId,Iterable[PolyBase.PolyBaseTree]]()
      val mergedCoords = new LinkedHashMap[CellId,Array[Int]]()
      for (val t <- dt) {
        val cellId = t.top.cellRevision.d.cellId
        val ba = layoutMerger.mergeLocalLayerToByteArray(cellId, layer)
        if (ba != null) {
          mergedTrees.put(cellId, byteArray2tree(ba))
          mergedCoords.put(cellId, layoutMerger.byteArray2coordArray(ba))
        } else {
          mergedTrees.put(cellId, Iterable.empty)
          mergedCoords.put(cellId, Array.empty)
        }
      }
      println(countHier(r => treesSize(mergedTrees(r.d.cellId), p => 1)) + " merged polygons")
      println(countFlat(r => treesSize(mergedTrees(r.d.cellId), p => 1)) + " merged polygon insts")
      println(countHier(r => treesSize(mergedTrees(r.d.cellId), p => p.getPoints.length)) + " merged points")
      println(countFlat(r => treesSize(mergedTrees(r.d.cellId), p => p.getPoints.length)) + " merged point insts")

      val rotate = false
      val fileName = layer.getName + ".dm"
      val out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))
      out.writeBoolean(rotate)
      layoutMerger.mergeLayer(mergedCoords, topCell.getId, layer, rotate, out)
      out.close

      val inpS = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)))
      assert(inpS.readBoolean == rotate)
      val up = new UnloadPolys
      val trees: Iterable[PolyBase.PolyBaseTree] = JavaConversions.asIterable(up.loop(inpS, false))
      println(treesSize(trees, p => 1) + " merged polygons")
      println(treesSize(trees, p => p.getPoints.length) + " merged polygons")
      inpS.close()
    }
  }

}
