/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EClassLoader.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.sandbox;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;

/**
 * A ClassLoader which loads "electric.jar" (possibly with old Electric version) and finds there
 * some important classes, consructors, fields and methods.
 */
class EClassLoader extends URLClassLoader {

    protected final Class<?> classMain                  = loadElectricClass("Main");
    protected final Class<?> classMainUserInterfaceDummy= loadElectricClass("Main$UserInterfaceDummy");
    protected final Class<?> classUndo                  = loadElectricClass("database.change.Undo");
    protected final Class<?> classEGraphics             = loadElectricClass("database.geometry.EGraphics");
    protected final Class<?> classEGraphicsOutline      = loadElectricClass("database.geometry.EGraphics$Outline");
    protected final Class<?> classPoly                  = loadElectricClass("database.geometry.Poly");
    protected final Class<?> classPolyType              = loadElectricClass("database.geometry.Poly$Type");
    protected final Class<?> classCell                  = loadElectricClass("database.hierarchy.Cell");
    protected final Class<?> classCellVersionGroup      = loadElectricClass("database.hierarchy.Cell$VersionGroup");
    protected final Class<?> classEDatabase             = loadElectricClass("database.hierarchy.EDatabase");
    protected final Class<?> classLibrary               = loadElectricClass("database.hierarchy.Library");
    protected final Class<?> classNodeProto             = loadElectricClass("database.prototype.NodeProto");
    protected final Class<?> classPref                  = loadElectricClass("database.text.Pref");
    protected final Class<?> classSetting               = loadElectricClass("database.text.Setting");
    protected final Class<?> classVersion               = loadElectricClass("database.text.Version");
    protected final Class<?> classNodeInst              = loadElectricClass("database.topology.NodeInst");
    protected final Class<?> classAbstractTextDescriptor= loadElectricClass("database.variable.AbstractTextDescriptor");
    protected final Class<?> classElectricObject        = loadElectricClass("database.variable.ElectricObject");
    protected final Class<?> classEditWindow0           = loadElectricClass("database.variable.EditWindow0");
    protected final Class<?> classEditWindow_           = loadElectricClass("database.variable.EditWindow_");
    protected final Class<?> classTextDescriptor        = loadElectricClass("database.variable.TextDescriptor");
    protected final Class<?> classTextDescriptorSize    = loadElectricClass("database.variable.TextDescriptor$Size", "database.variable.AbstractTextDescriptor$Size");
    protected final Class<?> classUserInterface         = loadElectricClass("database.variable.UserInterface");
    protected final Class<?> classVarContext            = loadElectricClass("database.variable.VarContext");
    protected final Class<?> classVariable              = loadElectricClass("database.variable.Variable");
    protected final Class<?> classArcProto              = loadElectricClass("technology.ArcProto", "database.prototype.ArcProto");
    protected final Class<?> classArcProtoFunction      = loadElectricClass("technology.ArcProto$Function", "database.prototype.ArcProto$Function");
    protected final Class<?> classDRCTemplate           = loadElectricClass("technology.DRCTemplate");
    protected final Class<?> classDRCTemplateDRCMode    = loadElectricClass("technology.DRCTemplate$DRCMode");
    protected final Class<?> classDRCTemplateDRCRuleType= loadElectricClass("technology.DRCTemplate$DRCRuleType");
    protected final Class<?> classEdgeH                 = loadElectricClass("technology.EdgeH");
    protected final Class<?> classEdgeV                 = loadElectricClass("technology.EdgeV");
    protected final Class<?> classFoundry               = loadElectricClass("technology.Foundry");
    protected final Class<?> classLayer                 = loadElectricClass("technology.Layer");
    protected final Class<?> classLayerFunction         = loadElectricClass("technology.Layer$Function");
    protected final Class<?> classLayerFunctionSet      = loadElectricClass("technology.Layer$Function$Set");
    protected final Class<?> classPrimitiveArc          = loadElectricClass("technology.PrimitiveArc");
    protected final Class<?> classPrimitiveNode         = loadElectricClass("technology.PrimitiveNode");
    protected final Class<?> classPrimitiveNodeFunction = loadElectricClass("technology.PrimitiveNode$Function", "database.prototype.NodeProto$Function");
    protected final Class<?> classPrimitiveNodeNodeSizeRule = loadElectricClass("technology.PrimitiveNode$NodeSizeRule");
    protected final Class<?> classPrimitivePort         = loadElectricClass("technology.PrimitivePort");
    protected final Class<?> classTechnology            = loadElectricClass("technology.Technology");
    protected final Class<?> classTechnologyArcLayer    = loadElectricClass("technology.Technology$ArcLayer");
    protected final Class<?> classTechnologyNodeLayer   = loadElectricClass("technology.Technology$NodeLayer");
    protected final Class<?> classTechnologyTechPoint   = loadElectricClass("technology.Technology$TechPoint");
    protected final Class<?> classSizeOffset            = loadElectricClass("technology.SizeOffset");
    protected final Class<?> classAbstractUserInterface = loadElectricClass("tool.AbstractUserInterface");
    protected final Class<?> classJob                   = loadElectricClass("tool.Job");
    protected final Class<?> classJobMode               = loadElectricClass("tool.Job$Mode");
    protected final Class<?> classTool                  = loadElectricClass("tool.Tool");
    protected final Class<?> classERC                   = loadElectricClass("tool.erc.ERC");
    protected final Class<?> classUser                  = loadElectricClass("tool.user.User");
    protected final Class<?> classEditWindow            = loadElectricClass("tool.user.ui.EditWindow");

    protected final Field Main_NOTHREADING              = getField(classMain, "NOTHREADING");
    protected final Field Cell_versionGroup             = getDeclaredField(classCell, "versionGroup");
    protected final Field Pref_allPrefs                 = getDeclaredField(classPref, "allPrefs");
    protected final Field Pref_prefs                    = getDeclaredField(classPref, "prefs");
    protected final Field Setting_prefs                 = getDeclaredField(classSetting, "prefs");
    protected final Field ArcProto_layers               = getDeclaredField(classPrimitiveArc != null ? classPrimitiveArc : classArcProto, "layers");
    protected final Field DRCTemplate_ruleName          = getField(classDRCTemplate, "ruleName");
    protected final Field DRCTemplate_when              = getField(classDRCTemplate, "when");
    protected final Field DRCTemplate_ruleType          = getField(classDRCTemplate, "ruleType");
    protected final Field DRCTemplate_name1             = getField(classDRCTemplate, "name1");
    protected final Field DRCTemplate_name2             = getField(classDRCTemplate, "name2");
    protected final Field DRCTemplate_value1            = getField(classDRCTemplate, "value1");
    protected final Field DRCTemplate_value2            = getField(classDRCTemplate, "value2");
    protected final Field DRCTemplate_values            = getField(classDRCTemplate, "values");
    protected final Field DRCTemplate_maxWidth          = getField(classDRCTemplate, "maxWidth");
    protected final Field DRCTemplate_minLength         = getField(classDRCTemplate, "minLength");
    protected final Field DRCTemplate_nodeName          = getField(classDRCTemplate, "nodeName");
    protected final Field DRCTemplate_multiCuts         = getField(classDRCTemplate, "multiCuts");
    protected final Field PrimitiveNode_LOWVTBIT        = getField(classPrimitiveNode, "LOWVTBIT");
    protected final Field PrimitiveNode_HIGHVTBIT       = getField(classPrimitiveNode, "HIGHVTBIT");
    protected final Field PrimitiveNode_NATIVEBIT       = getField(classPrimitiveNode, "NATIVEBIT");
    protected final Field PrimitiveNode_OD18BIT         = getField(classPrimitiveNode, "OD18BIT");
    protected final Field PrimitiveNode_OD25BIT         = getField(classPrimitiveNode, "OD25BIT");
    protected final Field PrimitiveNode_OD33BIT         = getField(classPrimitiveNode, "OD33BIT");
    protected final Field Job_NOTHREADING               = getField(classJob, "NOTHREADING");
    protected final Field JobMode_SERVER                = getField(classJobMode, "SERVER");
    protected final Field JobMode_BATCH                 = getField(classJobMode, "BATCH");
    protected final Field JobMode_CLIENT                = getField(classJobMode, "CLIENT");
    protected final Field ERC_tool                      = getDeclaredField(classERC, "tool");

    protected final Constructor MainUserInterfaceDummy_constructor = getDeclaredConstructor(classMainUserInterfaceDummy);
    protected final Constructor CellVersionGroup_constructor       = getDeclaredConstructor(classCellVersionGroup);

    protected final Method Undo_changesQuiet = getMethod(classUndo, "changesQuiet", Boolean.TYPE);
    protected final Method EGraphics_getColor = getMethod(classEGraphics, "getColor");
    protected final Method EGraphics_getForeground = getMethod(classEGraphics, "getForeground");
    protected final Method EGraphics_getOpacity = getMethod(classEGraphics, "getOpacity");
    protected final Method EGraphics_getOutlined = getMethod(classEGraphics, "getOutlined");
    protected final Method EGraphics_getPattern = getMethod(classEGraphics, "getPattern");
    protected final Method EGraphics_getTransparentLayer = getMethod(classEGraphics, "getTransparentLayer");
    protected final Method EGraphics_isOutlinedOnDisplay = getMethod(classEGraphics, "isOutlinedOnDisplay");
    protected final Method EGraphics_isOutlinedOnPrinter = getMethod(classEGraphics, "isOutlinedOnPrinter");
    protected final Method EGraphics_isPatternedOnDisplay = getMethod(classEGraphics, "isPatternedOnDisplay");
    protected final Method EGraphics_isPatternedOnPrinter = getMethod(classEGraphics, "isPatternedOnPrinter");
    protected final Method Poly_getPoints = getMethod(classPoly, "getPoints");
    protected final Method Poly_getStyle = getMethod(classPoly, "getStyle");
    protected final Method Cell_lowLevelAllocate = getDeclaredMethod(classCell, "lowLevelAllocate", classLibrary);
    protected final Method Cell_lowLevelLink = getDeclaredMethod(classCell, "lowLevelLink");
    protected final Method Cell_lowLevelPopulate = getDeclaredMethod(classCell, "lowLevelPopulate", String.class);
    protected final Method Cell_newInstance = getMethod(classCell, "newInstance", classLibrary, String.class);
    protected final Method CellVersionGroup_add = getDeclaredMethod(classCellVersionGroup, "add", classCell);
    protected final Method EDatabase_lock = getMethod(classEDatabase, "lock", Boolean.TYPE);
    protected final Method EDatabase_lowLevelBeginChanges = getMethod(classEDatabase, "lowLevelBeginChanging", classTool);
    protected final Method EDatabase_lowLevelSetCanChanging = getMethod(classEDatabase, "lowLevelSetCanChanging", classTool);
    protected final Method EDatabase_serverDatabase = getMethod(classEDatabase, "serverDatabase");
    protected final Method Library_getLibraries = getMethod(classLibrary, "getLibraries");
    protected final Method Library_getName = getMethod(classLibrary, "getName");
    protected final Method Library_newInstance = getMethod(classLibrary, "newInstance", String.class, URL.class);
    protected final Method Pref_getFactoryValue = getMethod(classPref, "getFactoryValue");
    protected final Method Pref_getMeaning = getMethod(classPref, "getMeaning");
    protected final Method Pref_getPrefName = getMethod(classPref, "getPrefName");
    protected final Method Setting_getFactoryValue = getMethod(classSetting, "getFactoryValue");
    protected final Method Setting_getPrefName = getMethod(classSetting, "getPrefName");
    protected final Method Setting_getSettings = getMethod(classSetting, "getSettings");
    protected final Method Setting_getXmlPath = getMethod(classSetting, "getXmlPath");
    protected final Method Version_getVersion = getMethod(classVersion, "getVersion");
    protected final Method NodeInst_getAngle = getMethod(classNodeInst, "getAngle");
    protected final Method NodeInst_getFunction = getMethod(classNodeInst, "getFunction");
    protected final Method NodeInst_getProto = getMethod(classNodeInst, "getProto");
    protected final Method NodeInst_newInstance1 = getMethod(classNodeInst, "newInstance", classNodeProto, Point2D.class, Double.TYPE, Double.TYPE, classCell);
    protected final Method NodeInst_newInstance2 = getMethod(classNodeInst, "newInstance", classNodeProto, Point2D.class, Double.TYPE, Double.TYPE, Integer.TYPE, classCell, String.class);
    protected final Method ElectricObject_getVariables = getMethod(classElectricObject, "getVariables");
    protected final Method TextDescriptor_getSize = getDeclaredMethod(classAbstractTextDescriptor != null ? classAbstractTextDescriptor : classTextDescriptor, "getSize");
    protected final Method TextDescriptorSize_getSize = getMethod(classTextDescriptorSize, "getSize");
    protected final Method Variable_getObject = getMethod(classVariable, "getObject");
    protected final Method Variable_getTextDescriptor = getMethod(classVariable, "getTextDescriptor");
    protected final Method EdgeH_getAdder = getMethod(classEdgeH, "getAdder");
    protected final Method EdgeH_getMultiplier = getMethod(classEdgeH, "getMultiplier");
    protected final Method EdgeV_getAdder = getMethod(classEdgeV, "getAdder");
    protected final Method EdgeV_getMultiplier = getMethod(classEdgeV, "getMultiplier");
    protected final Method ArcProto_getAngleIncrement = getMethod(classArcProto, "getAngleIncrement");
    protected final Method ArcProto_getAntennaRatio = getMethod(classArcProto, "getAntennaRatio");
    protected final Method ArcProto_getDefaultWidth = getMethod(classArcProto, "getDefaultWidth");
    protected final Method ArcProto_getDefaultLambdaBaseWidth = getMethod(classArcProto, "getDefaultLambdaBaseWidth");
    protected final Method ArcProto_getDefaultLambdaFullWidth = getMethod(classArcProto, "getDefaultLambdaFullWidth");
    protected final Method ArcProto_getFunction = getMethod(classArcProto, "getFunction");
    protected final Method ArcProto_getLambdaElibWidthOffset = getMethod(classArcProto, "getLambdaElibWidthOffset");
    protected final Method ArcProto_getLambdaWidthOffset = getMethod(classArcProto, "getLambdaWidthOffset");
    protected final Method ArcProto_getName = getMethod(classArcProto, "getName");
    protected final Method ArcProto_getWidthOffset = getMethod(classArcProto, "getWidthOffset");
    protected final Method ArcProto_isCurvable = getMethod(classArcProto, "isCurvable");
    protected final Method ArcProto_isExtended = getMethod(classArcProto, "isExtended");
    protected final Method ArcProto_isFixedAngle = getMethod(classArcProto, "isFixedAngle");
    protected final Method ArcProto_isNotUsed = getMethod(classArcProto, "isNotUsed");
    protected final Method ArcProto_isSkipSizeInPalette = getMethod(classArcProto, "isSkipSizeInPalette");
    protected final Method ArcProto_isSpecialArc = getMethod(classArcProto, "isSpecialArc");
    protected final Method ArcProto_isWipable = getMethod(classArcProto, "isWipable");
    protected final Method DRCTemplateDrcMode_mode = getMethod(classDRCTemplateDRCMode, "mode");
    protected final Method Foundry_getGDSLayers = getMethod(classFoundry, "getGDSLayers");
    protected final Method Foundry_getRules = getMethod(classFoundry, "getRules");
    protected final Method Layer_getCapacitance = getMethod(classLayer, "getCapacitance");
    protected final Method Layer_getCIFLayer = getMethod(classLayer, "getCIFLayer");
    protected final Method Layer_getDXFLayer = getMethod(classLayer, "getDXFLayer");
    protected final Method Layer_getDistance = getMethod(classLayer, "getDistance");
    protected final Method Layer_getEdgeCapacitance = getMethod(classLayer, "getEdgeCapacitance");
    protected final Method Layer_getFunction = getMethod(classLayer, "getFunction");
    protected final Method Layer_getFunctionExtras = getMethod(classLayer, "getFunctionExtras");
    protected final Method Layer_getGDSLayer = getMethod(classLayer, "getGDSLayer");
    protected final Method Layer_getGraphics = getMethod(classLayer, "getGraphics");
    protected final Method Layer_getHeight = getMethod(classLayer, "getHeight");
    protected final Method Layer_getName = getMethod(classLayer, "getName");
    protected final Method Layer_getNonPseudoLayer = getMethod(classLayer, "getNonPseudoLayer");
    protected final Method Layer_getPseudoLayer = getMethod(classLayer, "getPseudoLayer");
    protected final Method Layer_getResistance = getMethod(classLayer, "getResistance");
    protected final Method Layer_getSkillLayer = getMethod(classLayer, "getSkillLayer");
    protected final Method Layer_getThickness = getMethod(classLayer, "getThickness");
    protected final Method Layer_getTransparencyFactor = getMethod(classLayer, "getTransparencyFactor");
    protected final Method Layer_getTransparencyMode = getMethod(classLayer, "getTransparencyMode");
    protected final Method Layer_isPseudoLayer = getMethod(classLayer, "isPseudoLayer");
    protected final Method PrimitiveNode_getBaseRectangle = getMethod(classPrimitiveNode, "getBaseRectangle");
    protected final Method PrimitiveNode_getDefWidth = getMethod(classPrimitiveNode, "getDefWidth");
    protected final Method PrimitiveNode_getDefHeight = getMethod(classPrimitiveNode, "getDefHeight");
    protected final Method PrimitiveNode_getElectricalLayers = getMethod(classPrimitiveNode, "getElectricalLayers");
    protected final Method PrimitiveNode_getFullRectangle = getMethod(classPrimitiveNode, "getFullRectangle");
    protected final Method PrimitiveNode_getFunction = getMethod(classPrimitiveNode, "getFunction");
    protected final Method PrimitiveNode_getLayers = getMethod(classPrimitiveNode, "getLayers");
    protected final Method PrimitiveNode_getMinHeight = getMethod(classPrimitiveNode, "getMinHeight");
    protected final Method PrimitiveNode_getMinSizeRule = getMethod(classPrimitiveNode, "getMinSizeRule");
    protected final Method PrimitiveNode_getMinWidth = getMethod(classPrimitiveNode, "getMinWidth");
    protected final Method PrimitiveNode_getName = getMethod(classPrimitiveNode, "getName");
    protected final Method PrimitiveNode_getPorts = getMethod(classPrimitiveNode, "getPorts");
    protected final Method PrimitiveNode_getProtoSizeOffset = getMethod(classPrimitiveNode, "getProtoSizeOffset");
    protected final Method PrimitiveNode_getSizeCorrector = getDeclaredMethod(classPrimitiveNode, "getSizeCorrector", Integer.TYPE);
    protected final Method PrimitiveNode_getSpecialType = getMethod(classPrimitiveNode, "getSpecialType");
    protected final Method PrimitiveNode_getSpecialValues = getMethod(classPrimitiveNode, "getSpecialValues");
    protected final Method PrimitiveNode_getSpiceTemplate = getMethod(classPrimitiveNode, "getSpiceTemplate");
    protected final Method PrimitiveNode_isArcsShrink = getMethod(classPrimitiveNode, "isArcsShrink");
    protected final Method PrimitiveNode_isArcsWipe = getMethod(classPrimitiveNode, "isArcsWipe");
    protected final Method PrimitiveNode_isCanBeZeroSize = getMethod(classPrimitiveNode, "isCanBeZeroSize");
    protected final Method PrimitiveNode_isEdgeSelect = getMethod(classPrimitiveNode, "isEdgeSelect");
    protected final Method PrimitiveNode_isLockedPrim = getMethod(classPrimitiveNode, "isLockedPrim");
    protected final Method PrimitiveNode_isNodeBitOn = getMethod(classPrimitiveNode, "isNodeBitOn", Integer.TYPE);
    protected final Method PrimitiveNode_isNotUsed = getMethod(classPrimitiveNode, "isNotUsed");
    protected final Method PrimitiveNode_isSkipSizeInPalette = getMethod(classPrimitiveNode, "isSkipSizeInPalette");
    protected final Method PrimitiveNode_isSquare = getMethod(classPrimitiveNode, "isSquare");
    protected final Method PrimitiveNode_isWipeOn1or2 = getMethod(classPrimitiveNode, "isWipeOn1or2");
    protected final Method PrimitiveNodeNodeSizeRule_getHeight = getMethod(classPrimitiveNodeNodeSizeRule, "getHeight");
    protected final Method PrimitiveNodeNodeSizeRule_getRuleName = getMethod(classPrimitiveNodeNodeSizeRule, "getRuleName");
    protected final Method PrimitiveNodeNodeSizeRule_getWidth = getMethod(classPrimitiveNodeNodeSizeRule, "getWidth");
    protected final Method PrimitivePort_getAngle = getMethod(classPrimitivePort, "getAngle");
    protected final Method PrimitivePort_getAngleRange = getMethod(classPrimitivePort, "getAngleRange");
    protected final Method PrimitivePort_getBottom = getMethod(classPrimitivePort, "getBottom");
    protected final Method PrimitivePort_getConnections = getMethod(classPrimitivePort, "getConnections");
    protected final Method PrimitivePort_getLeft = getMethod(classPrimitivePort, "getLeft");
    protected final Method PrimitivePort_getName = getMethod(classPrimitivePort, "getName");
    protected final Method PrimitivePort_getRight = getMethod(classPrimitivePort, "getRight");
    protected final Method PrimitivePort_getTop = getMethod(classPrimitivePort, "getTop");
    protected final Method PrimitivePort_getTopology = getMethod(classPrimitivePort, "getTopology");
    protected final Method PrimitivePort_lowLevelGetUserbits = getMethod(classPrimitivePort, "lowLevelGetUserbits");
    protected final Method SizeOffset_getHighXOffset = getMethod(classSizeOffset, "getHighXOffset");
    protected final Method SizeOffset_getHighYOffset = getMethod(classSizeOffset, "getHighYOffset");
    protected final Method SizeOffset_getLowXOffset = getMethod(classSizeOffset, "getLowXOffset");
    protected final Method SizeOffset_getLowYOffset = getMethod(classSizeOffset, "getLowYOffset");
    protected final Method Technology_findArcProto = getMethod(classTechnology, "findArcProto", String.class);
    protected final Method Technology_findTechnology = getMethod(classTechnology, "findTechnology", String.class);
    protected final Method Technology_getArcs = getMethod(classTechnology, "getArcs");
    protected final Method Technology_getColorMap = getMethod(classTechnology, "getColorMap");
    protected final Method Technology_getFoundries = getMethod(classTechnology, "getFoundries");
    protected final Method Technology_getLayers = getMethod(classTechnology, "getLayers");
    protected final Method Technology_getMinResistance = getMethod(classTechnology, "getMinResistance");
    protected final Method Technology_getMinCapacitance = getMethod(classTechnology, "getMinCapacitance");
    protected final Method Technology_getNodes = getMethod(classTechnology, "getNodes");
    protected final Method Technology_getNodesGrouped1 = getMethod(classTechnology, "getNodesGrouped");
    protected final Method Technology_getNodesGrouped2 = getMethod(classTechnology, "getNodesGrouped", classCell);
    protected final Method Technology_getNumMetals = getMethod(classTechnology, "getNumMetals");
    protected final Method Technology_getNumTransparentLayers = getMethod(classTechnology, "getNumTransparentLayers");
    protected final Method Technology_getOldArcNames = getMethod(classTechnology, "getOldArcNames");
    protected final Method Technology_getOldNodeNames = getMethod(classTechnology, "getOldNodeNames");
    protected final Method Technology_getPrefFoundry = getMethod(classTechnology, "getPrefFoundry");
    protected final Method Technology_getScale = getMethod(classTechnology, "getScale");
    protected final Method Technology_getResolution = getMethod(classTechnology, "getResolution");
    protected final Method Technology_getShapeOfNode1 = getMethod(classTechnology, "getShapeOfNode", classNodeInst, Boolean.TYPE, Boolean.TYPE, classLayerFunctionSet);
    protected final Method Technology_getShapeOfNode2 = getMethod(classTechnology, "getShapeOfNode", classNodeInst, classEditWindow0, classVarContext, Boolean.TYPE, Boolean.TYPE, List.class);
    protected final Method Technology_getShapeOfNode3 = getMethod(classTechnology, "getShapeOfNode", classNodeInst, classEditWindow_, classVarContext, Boolean.TYPE, Boolean.TYPE, List.class);
    protected final Method Technology_getShapeOfNode4 = getMethod(classTechnology, "getShapeOfNode", classNodeInst, classEditWindow, classVarContext, Boolean.TYPE, Boolean.TYPE, List.class);
    protected final Method Technology_getShapeOfNode5 = getMethod(classTechnology, "getShapeOfNode", classNodeInst, classEditWindow, Boolean.TYPE, Boolean.TYPE, List.class);
    protected final Method Technology_getShapeOfNode6 = getMethod(classTechnology, "getShapeOfNode", classNodeInst, classEditWindow, Boolean.TYPE, Boolean.TYPE);
    protected final Method Technology_getSpiceHeaderLevel1 = getMethod(classTechnology, "getSpiceHeaderLevel1");
    protected final Method Technology_getSpiceHeaderLevel2 = getMethod(classTechnology, "getSpiceHeaderLevel2");
    protected final Method Technology_getSpiceHeaderLevel3 = getMethod(classTechnology, "getSpiceHeaderLevel3");
    protected final Method Technology_getTechDesc = getMethod(classTechnology, "getTechDesc");
    protected final Method Technology_getTechName = getMethod(classTechnology, "getTechName");
    protected final Method Technology_getTechShortName = getMethod(classTechnology, "getTechShortName");
    protected final Method Technology_getTechnologies = getMethod(classTechnology, "getTechnologies");
    protected final Method Technology_initAllTechnologies = getMethod(classTechnology, "initAllTechnologies");
    protected final Method Technology_isScaleRelevant = getMethod(classTechnology, "isScaleRelevant");
    protected final Method TechnologyArcLayer_getGridExtend = getDeclaredMethod(classTechnologyArcLayer, "getGridExtend");
    protected final Method TechnologyArcLayer_getLambdaOffset = getDeclaredMethod(classTechnologyArcLayer, "getLambdaOffset");
    protected final Method TechnologyArcLayer_getLayer = getDeclaredMethod(classTechnologyArcLayer, "getLayer");
    protected final Method TechnologyArcLayer_getOffset = getDeclaredMethod(classTechnologyArcLayer, "getOffset");
    protected final Method TechnologyArcLayer_getStyle = getDeclaredMethod(classTechnologyArcLayer, "getStyle");
    protected final Method TechnologyNodeLayer_getLayer = getMethod(classTechnologyNodeLayer, "getLayer");
    protected final Method TechnologyNodeLayer_getMulticutSizeX = getMethod(classTechnologyNodeLayer, "getMulticutSizeX");
    protected final Method TechnologyNodeLayer_getMulticutSizeY = getMethod(classTechnologyNodeLayer, "getMulticutSizeY");
    protected final Method TechnologyNodeLayer_getMulticutSep1D = getMethod(classTechnologyNodeLayer, "getMulticutSep1D");
    protected final Method TechnologyNodeLayer_getMulticutSep2D = getMethod(classTechnologyNodeLayer, "getMulticutSep2D");
    protected final Method TechnologyNodeLayer_getPoints = getMethod(classTechnologyNodeLayer, "getPoints");
    protected final Method TechnologyNodeLayer_getPortNum = getMethod(classTechnologyNodeLayer, "getPortNum");
    protected final Method TechnologyNodeLayer_getRepresentation = getMethod(classTechnologyNodeLayer, "getRepresentation");
    protected final Method TechnologyNodeLayer_getSerpentineExtentB = getMethod(classTechnologyNodeLayer, "getSerpentineExtentB");
    protected final Method TechnologyNodeLayer_getSerpentineExtentT = getMethod(classTechnologyNodeLayer, "getSerpentineExtentT");
    protected final Method TechnologyNodeLayer_getSerpentineLWidth = getMethod(classTechnologyNodeLayer, "getSerpentineLWidth");
    protected final Method TechnologyNodeLayer_getSerpentineRWidth = getMethod(classTechnologyNodeLayer, "getSerpentineRWidth");
    protected final Method TechnologyNodeLayer_getStyle = getMethod(classTechnologyNodeLayer, "getStyle");
    protected final Method TechnologyNodeLayer_isPseudoLayer = getMethod(classTechnologyNodeLayer, "isPseudoLayer");
    protected final Method TechnologyTechPoint_getX = getMethod(classTechnologyTechPoint, "getX");
    protected final Method TechnologyTechPoint_getY = getMethod(classTechnologyTechPoint, "getY");
    protected final Method Job_initJobManager1 = getMethod(classJob, "initJobManager", Integer.TYPE, classJob, Object.class, String.class);
    protected final Method Job_initJobManager2 = getMethod(classJob, "initJobManager", Integer.TYPE, classJob, Object.class);
    protected final Method Job_initJobManager3 = getMethod(classJob, "initJobManager", Integer.TYPE, classJob);
    protected final Method Job_setThreadMode1 = getMethod(classJob, "setThreadMode", classJobMode, classAbstractUserInterface);
    protected final Method Job_setThreadMode2 = getMethod(classJob, "setThreadMode", classJobMode, classUserInterface);
    protected final Method Job_startJob = getMethod(classJob, "startJob");
    protected final Method Tool_initAllTools = getDeclaredMethod(classTool, "initAllTools");
    protected final Method Tool_initProjectSettings = getDeclaredMethod(classTool, "initProjectSettings");
    protected final Method ERC_getAntennaRatio = getMethod(classERC, "getAntennaRatio", classArcProto);
    protected final Method User_getUserTool = getMethod(classUser, "getUserTool");

    protected final HashMap<Object,EGraphics.Outline> EGraphicsOutlines = new HashMap<Object,EGraphics.Outline>();
    protected final HashMap<Object,Poly.Type> PolyTypes = new HashMap<Object,Poly.Type>();
    protected final HashMap<Object,DRCTemplate.DRCMode> DRCTemplateDRCModes = new HashMap<Object,DRCTemplate.DRCMode>();
    protected final HashMap<Object,DRCTemplate.DRCRuleType> DRCTemplateDRCRuleTypes = new HashMap<Object,DRCTemplate.DRCRuleType>();
    protected final HashMap<Object,Layer.Function> LayerFunctions = new HashMap<Object,Layer.Function>();
    protected final HashMap<Object,ArcProto.Function> ArcProtoFunctions = new HashMap<Object,ArcProto.Function>();
    protected final HashMap<Object,PrimitiveNode.Function> PrimitiveNodeFunctions = new HashMap<Object,PrimitiveNode.Function>();

    /** Creates a new instance of EClassLoader */
    public EClassLoader(URL electricJar) throws IOException, ClassNotFoundException, IllegalAccessException {
        super(new URL[] { checkConnection(electricJar) }, ClassLoader.getSystemClassLoader().getParent());

        assert getClass().getClassLoader().getParent() == getParent();
        if (classEGraphicsOutline != null) {
            for (EGraphics.Outline o: EGraphics.Outline.class.getEnumConstants()) {
                Field f = getField(classEGraphicsOutline, o.name());
                if (f == null) continue;
                EGraphics.Outline old = EGraphicsOutlines.put(f.get(null), o);
                assert old == null;
            }
        }
        for (Poly.Type style: Poly.Type.class.getEnumConstants()) {
            Field f = getField(classPolyType, style.name());
            if (f == null) continue;
            Poly.Type old = PolyTypes.put(f.get(null), style);
            assert old == null;
        }
        if (classDRCTemplate != null) {
            for (DRCTemplate.DRCMode mode: DRCTemplate.DRCMode.class.getEnumConstants()) {
                Field f = getField(classDRCTemplateDRCMode, mode.name());
                if (f == null) continue;
                DRCTemplate.DRCMode old = DRCTemplateDRCModes.put(f.get(null), mode);
                assert old == null;
            }
            for (DRCTemplate.DRCRuleType type: DRCTemplate.DRCRuleType.class.getEnumConstants()) {
                Field f = getField(classDRCTemplateDRCRuleType, type.name());
                if (f == null) continue;
                DRCTemplate.DRCRuleType old = DRCTemplateDRCRuleTypes.put(f.get(null), type);
                assert old == null;
            }
        }
        for (Layer.Function fun: Layer.Function.class.getEnumConstants()) {
            Field f = getField(classLayerFunction, fun.name());
            if (f == null) continue;
            Layer.Function old = LayerFunctions.put(f.get(null), fun);
            assert old == null;
        }
        for (ArcProto.Function fun: ArcProto.Function.class.getEnumConstants()) {
            Field f = getField(classArcProtoFunction, fun.name());
            if (f == null) continue;
            ArcProto.Function old = ArcProtoFunctions.put(f.get(null), fun);
            assert old == null;
        }
        for (PrimitiveNode.Function fun: PrimitiveNode.Function.class.getEnumConstants()) {
            Field f = getField(classPrimitiveNodeFunction, fun.name());
            if (f == null) continue;
            PrimitiveNode.Function old = PrimitiveNodeFunctions.put(f.get(null), fun);
            assert old == null;
        }
    }

    private static URL checkConnection(URL url) throws IOException {
        url.openStream().close();
        return url;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("com.sun.electric.tool.sandbox."))
            return getClass().getClassLoader().loadClass(name);
        return super.loadClass(name, resolve);
//        if (!name.startsWith("com.sun.electric.") || name.startsWith("com.sun.electric.tool.sandbox."))
//            return super.loadClass(name, resolve);
//        Class c = findLoadedClass(name);
//        if (c == null || c.getClassLoader() != this)
//            c = findClass(name);
//        if (resolve)
//            resolveClass(c);
//        return c;
    }

//    @Override
//    public URL getResource(String name) {
//        if (!name.startsWith("com/sun/electric/") || name.startsWith("com/sun/electric/tool/sandbox/"))
//            return super.getResource(name);
//        return findResource(name);
//    }
//
//    @Override
//    public Enumeration<URL> getResources(String name) throws IOException {
//        if (!name.startsWith("com/sun/electric/") || name.startsWith("com/sun/electric/tool/sandbox/"))
//            return super.getResources(name);
//        return findResources(name);
//    }

    /*
     * Define class in this class loader from resource class loader found
     * in class loader's class loader.
     */
    protected synchronized Class<?> defineClass(String name) throws ClassNotFoundException {
        String resourceName = name.replaceAll("\\.", "/") + ".class";
        URL url = getClass().getClassLoader().getResource(resourceName);
        try {
            InputStream in = url.openStream();
            int len = in.available();
            byte[] ba = new byte[len];
            in.read(ba);
            in.close();
            return defineClass(name, ba, 0, len);
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
    }

    private Class<?> loadElectricClass(String ... className) {
        for (String s: className) {
            try {
                Class<?> c = loadClass("com.sun.electric." + s);
                if (c != null)
                    return c;
            } catch (ClassNotFoundException e) {
            }
        }
        return null;
    }

    private Field getField(Class<?> c, String fieldName) {
        Field f = null;
        try {
            if (c != null)
                f = c.getField(fieldName);
            if (f != null)
                f.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
        return f;
    }

    private Field getDeclaredField(Class<?> c, String fieldName) {
        Field f = null;
        try {
            if (c != null)
                f = c.getDeclaredField(fieldName);
            if (f != null)
                f.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
        return f;
    }

    private Method getMethod(Class<?> c, String methodName, Class<?>... parameterTypes) {
        Method m = null;
        try {
            if (c != null)
                m = c.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
        }
        return m;
    }

    private Method getDeclaredMethod(Class<?> c, String methodName, Class<?>... parameterTypes) {
        Method m = null;
        try {
            if (c != null)
                m = c.getDeclaredMethod(methodName, parameterTypes);
            if (m != null)
                m.setAccessible(true);
        } catch (NoSuchMethodException e) {
        }
        return m;
    }

    protected Constructor getDeclaredConstructor(Class<?> c, Class<?>... parameterTypes) {
        Constructor m = null;
        try {
            if (c != null)
                m = c.getDeclaredConstructor(parameterTypes);
            if (m != null)
                m.setAccessible(true);
        } catch (NoSuchMethodException e) {
        }
        return m;
    }
}
