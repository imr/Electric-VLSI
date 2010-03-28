package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.geometry.DBMath;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.DRCTemplate.DRCRuleType;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.XMLRules;
import com.sun.electric.tool.Job;

/** The TechType class holds technology dependent information for the layout
 * generators. Most of the information is available from public static methods.
 * <p> The TechType class queries the appropriate Technology object to get 
 * references to prototypes commonly used by the layout generators such as the 
 * metal 1 arc proto or the metal 1 pin proto. The Tech class also holds 
 * technology dependant dimensions such as the width of a diffusion contact.
 * <p>  The TechType class serves two purposes. First, it makes it convenient
 *  to access technology dependent information. Second, it hides foundry 
 *  specific information that we're not allowed to distribute as open source 
 *  software. */
public abstract class TechType implements Serializable {
    private static class ArcPair implements Serializable {
        private static final long serialVersionUID = 0;

        private ArcProto arc1, arc2;
        public ArcPair(ArcProto a1, ArcProto a2) {arc1=a1; arc2=a2;}
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArcPair)) return false;
            ArcPair ap = (ArcPair)o;
            if (ap.arc1==arc1 && ap.arc2==arc2) return true;
            if (ap.arc1==arc2 && ap.arc2==arc1) return true;
            return false;
        }
        @Override
        public int hashCode() {return arc1.hashCode()*arc2.hashCode();}
    }

    private class Transistor {
        private final PrimitiveNode pn;
        private PrimitivePort topPoly, bottomPoly, leftDiff, rightDiff;

        private Transistor(PrimitiveNode pn) {
            this.pn = pn;
            Technology.NodeLayer nmos_gate = findNodeLayer(pn, lgate);
            if (nmos_gate == null) {
                nmos_gate = findNodeLayer(pn, lp1);
            }
            boolean rotate = nmos_gate.getTopEdge().getGridAdder() - nmos_gate.getBottomEdge().getGridAdder() <
                    nmos_gate.getRightEdge().getGridAdder() - nmos_gate.getLeftEdge().getGridAdder();
            for (Iterator<PrimitivePort> it = pn.getPrimitivePorts(); it.hasNext(); ) {
                PrimitivePort pp = it.next();
                if (pp.getConnection().getFunction().isPoly()) {
                    if (rotate) {
                        if (pp.getRight().getGridAdder() > 0) {
                            topPoly = pp;
                        }  else {
                            bottomPoly = pp;
                        }
                    } else {
                        if (pp.getBottom().getGridAdder() > 0) {
                            topPoly = pp;
                        }  else {
                            bottomPoly = pp;
                        }
                    }
                } else if (pp.getConnection().getFunction().isDiffusion()) {
                    if (rotate) {
                        if (pp.getTop().getGridAdder() > 0) {
                            leftDiff = pp;
                        }  else {
                            rightDiff = pp;
                        }
                    } else {
                        if (pp.getRight().getGridAdder() > 0) {
                            rightDiff = pp;
                        }  else {
                            leftDiff = pp;
                        }
                    }

                }
            }
            assert topPoly != null && bottomPoly != null && leftDiff != null && rightDiff != null;
        }
    }

	//----------------------------- public data ----------------------------------
	private static final Variable.Key ATTR_X = Variable.newKey("ATTR_X");
	private static final Variable.Key ATTR_S = Variable.newKey("ATTR_S");
	private static final Variable.Key ATTR_SN = Variable.newKey("ATTR_SN");
	private static final Variable.Key ATTR_SP = Variable.newKey("ATTR_SP");

    private final Technology generic = Technology.findTechnology("generic");
    private final Technology technology;
    private final XMLRules drcRules;
    private final PrimitiveNode essentialBounds =
        generic.findNodeProto("Essential-Bounds");
    private final PrimitiveNode facetCenter =
        generic.findNodeProto("Facet-Center");

    private final int nbLay;
    private Layer lgate;
    private final Layer lp1;
    private final Layer[] lmets;
    private final ArcProto[] arcs;
    private final PrimitiveNode[] vias;
    private final HashMap<ArcPair,PrimitiveNode> viaMap = new HashMap<ArcPair,PrimitiveNode>();

    /** layers
     *
     * Poly and metal are considered to be routing layers.  In contrast
     * we assume we never want to route in diffusion or well.  This
     * allows us to assign a unique "height" to each of the routing
     * layers.  Layers at the same height can connect directly.  Layers
     * at adjacent heights can connect using vias.
     *
     * For now, well and diffusion don't have heights. */
    private final ArcProto pdiff, ndiff, p1, m1, m2, m3, m4, m5, m6, m7, m8, m9,
        ndiff18, pdiff18, ndiff25, pdiff25, ndiff33, pdiff33;

    /** layer pins */
    private final PrimitiveNode ndpin, pdpin, p1pin, m1pin, m2pin, m3pin,
        m4pin, m5pin, m6pin, m7pin, m8pin, m9pin;

    /** vias */
    private final PrimitiveNode nwm1, pwm1, nwm1Y, pwm1Y, ndm1, pdm1, p1m1,
        m1m2, m2m3, m3m4, m4m5, m5m6, m6m7, m7m8, m8m9;

    /** Transistors */
    private final boolean rotateTransistors;
    private final Transistor nmos, pmos, nmos18, pmos18, nmos25, pmos25,
        nmos33, pmos33;
    // nvth, pvth, nvtl, pvtl, nnat, pnat;

    /** special threshold transistor contacts */
    private final PrimitiveNode nmos18contact, pmos18contact, nmos25contact,
        pmos25contact, nmos33contact, pmos33contact;

    /** Pure layer nodes for Well and Select */
    private final PrimitiveNode nwellNode, pwellNode;

    /** Layer nodes are sometimes used to patch notches */
    private final PrimitiveNode m1Node, m2Node, m3Node, m4Node, m5Node, m6Node,
        m7Node, m8Node, m9Node, p1Node, pdNode, ndNode, pselNode, nselNode;

    /** Transistor layer nodes */
    private final PrimitiveNode od18Node, od25Node, od33Node, vthNode, vtlNode;

    //-------------------- Technology dependent dimensions  -------------------
    protected double
        // Gilda: gate length depending on foundry
        gateLength,

        // Wire offset from center of the poly contact to form a L-shape arc to gate
        offsetLShapePolyContact, offsetTShapePolyContact,

        // denote Select spacing rule
        selectSpace,

        // surround distance of select from active in transistor
        selectSurroundDiffInTrans, selectSurroundDiffAlongGateInTrans,

        // select surround over poly. PP/NP.R.1 in 90nm
        selectSurround;

    // RKao my first attempt to embed technology specific dimensions
    protected double
        wellSurroundDiff,
        gateExtendPastMOS,
        p1Width,
        p1ToP1Space,
        gateToGateSpace,
        gateToDiffContSpace,
        gateToDiffContSpaceDogBone,
        selectSurroundDiffInWellContact,	// select surround in well contacts
        selectSurroundDiffInActiveContact,	// select surround in active contacts
        m1MinArea, 							// min area rules, sq lambda
        polyContWidth,                      // width of poly of min size poly contact
        wellContWidth,                      // width of diff of min sized well contact
        diffContWidth,                      // width of diff of min sized diff contact
        diffCont_m1Width,					// width of m1 of min sized diff contact  
        diffContIncr;						// when diff cont increases by diffContIncr,
                                            // we get an additional cut

    //----------------------------- private methods  -----------------------------
    private static void error(boolean pred, String msg) {
        Job.error(pred, msg);
    }
    private Layer getMetalLayer(int l) {
        return l <= lmets.length ? lmets[l-1] : null;
    }
    private double getSpacing(Layer layer1, Layer layer2) {
        return drcRules.getSpacingRule(layer1, null, layer2, null, false, -1, -1.0, -1.0).getValue(0);
    }
    private ArcProto getArc(int n) {
        return n>(arcs.length-1) ? null : arcs[n];
    }
    private ArcProto findArc(Layer... layers) {
        for (Iterator<ArcProto> it = technology.getArcs(); it.hasNext(); ) {
            ArcProto ap = it.next();
            boolean allLayersFound = true;
            for (Layer layer: layers) {
                if (ap.indexOf(layer) < 0) {
                    allLayersFound = false;
                }
            }
            if (allLayersFound) {
                return ap;
            }
        }
        return null;
    }
//    private double getArcLayerGridExtend(ArcProto ap, Layer layer1, Layer layer2) {
//        if (ap == null) {
//            return Double.NaN;
//        }
//        int arcIndex1 = ap.indexOf(layer1);
//        int arcIndex2 = ap.indexOf(layer2);
//        if (arcIndex1 < 0 || arcIndex2 < 0) {
//            return Double.NaN;
//        }
//        return DBMath.gridToLambda((ap.getLayerGridExtend(arcIndex1) - ap.getLayerGridExtend(arcIndex2)));
//    }
    private PrimitiveNode findPureNode(Layer layer) {
        return layer != null ? layer.getPureLayerNode() : null;
    }

    private Technology.NodeLayer findNodeLayer(PrimitiveNode pn, Layer l) {
        if (pn == null) {
            return null;
        }
        Technology.NodeLayer[] nls = pn.getNodeLayers();
        for (Technology.NodeLayer nl: nls) {
            if (nl.getLayer() == l) {
                return nl;
            }
        }
        return null;
    }

    private Technology.NodeLayer findMulticut(PrimitiveNode pn) {
        if (pn == null) {
            return null;
        }
        Technology.NodeLayer[] nls = pn.getNodeLayers();
        for (Technology.NodeLayer nl: nls) {
            if (nl.getRepresentation() == Technology.NodeLayer.MULTICUTBOX) {
                return nl;
            }
        }
        return null;
    }

    private PrimitiveNode getVia(int n) {
        return n>(vias.length-1) ? null : vias[n];
    }
    /**
     * get the PrimitiveNode of a particular type that connects to the
     * complete set of wires given
     */
    private PrimitiveNode findNode(PrimitiveNode.Function type, ArcProto... arcs) {
        for (Iterator<PrimitiveNode> it=technology.getNodes(); it.hasNext();) {
            PrimitiveNode pn = it.next();
            boolean found = true;
            if (pn.getFunction() == type) {
                for (int j=0; j<arcs.length; j++) {
                    if (pn.connectsTo(arcs[j]) == null) {
                        found = false;
                        break;
                    }
                }
                if (found) return pn;
            }
        }
        return null;
    }
    private Transistor findTransistor(PrimitiveNode.Function type) {
        PrimitiveNode pn = findNode(type);
        if (pn == null) {
            return null;
        }
        return new Transistor(pn);
    }

    private PrimitiveNode findPin(ArcProto arc) {
        return arc==null ? null : arc.findPinProto();
    }

    private void putViaMap(ArcProto arc1, ArcProto arc2, PrimitiveNode via) {
        if (arc1==null || arc2==null || via==null) return;
        ArcPair ap = new ArcPair(arc1, arc2);
        error(viaMap.containsKey(ap), "two contacts for same pair of arcs?");
        viaMap.put(ap, via);
    }

    // initialize map from pair of layers to via that connects them
    private void initViaMap() {
        putViaMap(m1, m2, m1m2);
        putViaMap(m2, m3, m2m3);
        putViaMap(m3, m4, m3m4);
        putViaMap(m4, m5, m4m5);
        putViaMap(m5, m6, m5m6);
        putViaMap(m6, m7, m6m7);
        putViaMap(m7, m8, m7m8);
        putViaMap(m8, m9, m8m9);
        putViaMap(ndiff, m1, ndm1);
        putViaMap(pdiff, m1, pdm1);
        putViaMap(p1, m1, p1m1);
    }

    protected TechType(Technology techy) {
        // This error could happen when there are XML errors while uploading the technologies.
        error((techy==null), "Null technology in TechType constructor");
        
        // I can't break this into subroutines because most data members are
        // final.
        lmets = new Layer[techy.getNumMetals()];
        nbLay = 1 + lmets.length;
        technology = techy;
        drcRules = technology.getFactoryDesignRules();

        //--------------------------- initialize layers -----------------------
        arcs = new ArcProto[nbLay];
        lp1 = techy.findLayerFromFunction(Layer.Function.POLY1, -1);
        lgate = techy.findLayerFromFunction(Layer.Function.GATE, -1);
        arcs[0] = findArc(lp1);
        for (int i=1; i<nbLay; i++) {
            Layer lm = techy.findLayerFromFunction(Layer.Function.getMetal(i), -1);
            lmets[i - 1] = lm;
            arcs[i] = findArc(lm);
            error(arcs[i]==null, "No such arc: " + Layer.Function.getMetal(i) + " in technology " + techy.getTechName());
        }
        Layer lnwell = techy.findLayerFromFunction(Layer.Function.WELLN, 0);
        Layer lpwell = techy.findLayerFromFunction(Layer.Function.WELLP, 0);
        Layer lnd = techy.findLayerFromFunction(Layer.Function.DIFFN, 0);
        Layer lpd = techy.findLayerFromFunction(Layer.Function.DIFFP, 0);
        Layer lnsel = techy.findLayerFromFunction(Layer.Function.IMPLANTN, 0);
        Layer lpsel = techy.findLayerFromFunction(Layer.Function.IMPLANTP, 0);
        Layer lod18 = techy.findLayer("OD18");
        Layer lod25 = techy.findLayer("OD25");
        Layer lod33 = techy.findLayer("OD33");

        p1 = getArc(0);
        m1 = getArc(1);
        m2 = getArc(2);
        m3 = getArc(3);
        m4 = getArc(4);
        m5 = getArc(5);
        m6 = getArc(6);
        m7 = getArc(7);
        m8 = getArc(8);
        m9 = getArc(9);

        ndiff = findArc(lnd);
        pdiff = findArc(lpd);
        ndiff18 = findArc(lnd, lod18);
        pdiff18 = findArc(lpd, lod18);
        ndiff25 = findArc(lnd, lod25);
        pdiff25 = findArc(lpd, lod25);
        ndiff33 = findArc(lnd, lod33);
        pdiff33 = findArc(lpd, lod33);

        // Layer Nodes
        nwellNode = findPureNode(lnwell);
        pwellNode = findPureNode(lpwell);
        m1Node = findPureNode(getMetalLayer(1));
        m2Node = findPureNode(getMetalLayer(2));
        m3Node = findPureNode(getMetalLayer(3));
        m4Node = findPureNode(getMetalLayer(4));
        m5Node = findPureNode(getMetalLayer(5));
        m6Node = findPureNode(getMetalLayer(6));
        m7Node = findPureNode(getMetalLayer(7));
        m8Node = findPureNode(getMetalLayer(8));
        m9Node = findPureNode(getMetalLayer(9));
        p1Node = findPureNode(lp1);
        pdNode = findPureNode(lpd);
        ndNode = findPureNode(lnd);
        nselNode = findPureNode(lnsel);
        pselNode = findPureNode(lpsel);
        // transistor layers
        od18Node = findPureNode(lod18);
        od25Node = findPureNode(lod25);
        od33Node = findPureNode(lod33);
        vthNode = null;
        vtlNode = null;

        //--------------------------- initialize pins -------------------------
        pdpin = findPin(pdiff);
        ndpin = findPin(ndiff);
        p1pin = findPin(p1);
        m1pin = findPin(m1);
        m2pin = findPin(m2);
        m3pin = findPin(m3);
        m4pin = findPin(m4);
        m5pin = findPin(m5);
        m6pin = findPin(m6);
        m7pin = findPin(m7);
        m8pin = findPin(m8);
        m9pin = findPin(m9);

        //--------------------------- initialize vias -------------------------
        vias = new PrimitiveNode[nbLay - 1];
        for (int i = 0; i < nbLay - 1; i++) {
            vias[i] = findNode(PrimitiveNode.Function.CONTACT, arcs[i], arcs[i+1]);
            error(vias[i] == null, "No via for layer: " + arcs[i]);
        }

        p1m1 = getVia(0);
        m1m2 = getVia(1);
        m2m3 = getVia(2);
        m3m4 = getVia(3);
        m4m5 = getVia(4);
        m5m6 = getVia(5);
        m6m7 = getVia(6);
        m7m8 = getVia(7);
        m8m9 = getVia(8);

        ndm1 = findNode(PrimitiveNode.Function.CONTACT, ndiff, arcs[1]);
        pdm1 = findNode(PrimitiveNode.Function.CONTACT, pdiff, arcs[1]);
        nwm1 = findNode(PrimitiveNode.Function.WELL, arcs[1]);//techy.findNodeProto("Metal-1-N-Well-Con");
        pwm1 = findNode(PrimitiveNode.Function.SUBSTRATE, arcs[1]);//techy.findNodeProto("Metal-1-P-Well-Con");
        nwm1Y = techy.findNodeProto("Y-Metal-1-N-Well-Con");
        pwm1Y = techy.findNodeProto("Y-Metal-1-P-Well-Con");

        // initialize special threshold transistor contacts
        nmos18contact = findNode(PrimitiveNode.Function.CONTACT, ndiff18, arcs[1]);
        pmos18contact = findNode(PrimitiveNode.Function.CONTACT, pdiff18, arcs[1]);
        nmos25contact = findNode(PrimitiveNode.Function.CONTACT, ndiff25, arcs[1]);
        pmos25contact = findNode(PrimitiveNode.Function.CONTACT, pdiff25, arcs[1]);
        nmos33contact = findNode(PrimitiveNode.Function.CONTACT, ndiff33, arcs[1]);
        pmos33contact = findNode(PrimitiveNode.Function.CONTACT, pdiff33, arcs[1]);

        initViaMap();

        //------------------------ initialize transistors ---------------------
        nmos = findTransistor(PrimitiveNode.Function.TRANMOS);//techy.findNodeProto("N-Transistor");
        pmos = findTransistor(PrimitiveNode.Function.TRAPMOS);//techy.findNodeProto("P-Transistor");
        nmos18 = findTransistor(PrimitiveNode.Function.TRANMOSHV1);//techy.findNodeProto("OD18-N-Transistor");
        pmos18 = findTransistor(PrimitiveNode.Function.TRAPMOSHV1);//techy.findNodeProto("OD18-P-Transistor");
        nmos25 = findTransistor(PrimitiveNode.Function.TRANMOSHV2);//techy.findNodeProto("OD25-N-Transistor");
        pmos25 = findTransistor(PrimitiveNode.Function.TRAPMOSHV2);//techy.findNodeProto("OD25-P-Transistor");
        nmos33 = findTransistor(PrimitiveNode.Function.TRANMOSHV3);//techy.findNodeProto("OD33-N-Transistor");
        pmos33 = findTransistor(PrimitiveNode.Function.TRAPMOSHV3);//techy.findNodeProto("OD33-P-Transistor");

        Technology.NodeLayer nmos_gate = findNodeLayer(nmos.pn, lgate);
        if (nmos_gate == null) {
            nmos_gate = findNodeLayer(nmos.pn, lp1);
        }
        Technology.NodeLayer nmos_nd = findNodeLayer(nmos.pn, lnd);
        Technology.NodeLayer nmos_nsel = findNodeLayer(nmos.pn, lnsel);
        rotateTransistors = nmos_gate.getTopEdge().getGridAdder() - nmos_gate.getBottomEdge().getGridAdder() <
                nmos_gate.getRightEdge().getGridAdder() - nmos_gate.getLeftEdge().getGridAdder();
        double diffExtendAlongMos;
        if (rotateTransistors) {
            p1Width = gateLength = DBMath.gridToLambda(nmos_gate.getTopEdge().getGridAdder() - nmos_gate.getBottomEdge().getGridAdder());
            gateExtendPastMOS = DBMath.gridToLambda(nmos_gate.getRightEdge().getGridAdder() - nmos_nd.getRightEdge().getGridAdder());
            diffExtendAlongMos = DBMath.gridToLambda(nmos_nd.getTopEdge().getGridAdder() - nmos_gate.getTopEdge().getGridAdder());
            selectSurroundDiffInTrans = DBMath.gridToLambda(nmos_nsel.getTopEdge().getGridAdder() - nmos_nd.getTopEdge().getGridAdder());
            selectSurroundDiffAlongGateInTrans = DBMath.gridToLambda(nmos_nsel.getRightEdge().getGridAdder() - nmos_nd.getRightEdge().getGridAdder());
        } else {
            p1Width = gateLength = DBMath.gridToLambda(nmos_gate.getRightEdge().getGridAdder() - nmos_gate.getLeftEdge().getGridAdder());
            gateExtendPastMOS = DBMath.gridToLambda(nmos_gate.getTopEdge().getGridAdder() - nmos_nd.getTopEdge().getGridAdder());
            diffExtendAlongMos = DBMath.gridToLambda(nmos_nd.getRightEdge().getGridAdder() - nmos_gate.getRightEdge().getGridAdder());
            selectSurroundDiffInTrans = DBMath.gridToLambda(nmos_nsel.getRightEdge().getGridAdder() - nmos_nd.getRightEdge().getGridAdder());
            selectSurroundDiffAlongGateInTrans = DBMath.gridToLambda(nmos_nsel.getTopEdge().getGridAdder() - nmos_nd.getTopEdge().getGridAdder());
        }

        wellContWidth = Double.NaN;
        selectSurroundDiffInWellContact = Double.NaN;
        wellSurroundDiff = Double.NaN;
        if (nwm1Y == null) {
            Technology.NodeLayer nwm1_nwell = findNodeLayer(nwm1, lnwell);
            Technology.NodeLayer nwm1_nsel = findNodeLayer(nwm1, lnsel);
            Technology.NodeLayer nwm1_nd = findNodeLayer(nwm1, lnd);
            if (nwm1_nwell != null && nwm1_nd != null) {
                wellContWidth = DBMath.gridToLambda(nwm1_nd.getTopEdge().getGridAdder() - nwm1_nd.getBottomEdge().getGridAdder());
                selectSurroundDiffInWellContact = DBMath.gridToLambda(nwm1_nsel.getTopEdge().getGridAdder() - nwm1_nd.getTopEdge().getGridAdder());
                wellSurroundDiff = DBMath.gridToLambda(nwm1_nwell.getTopEdge().getGridAdder() - nwm1_nd.getTopEdge().getGridAdder());
            }
        }

        p1ToP1Space = getSpacing(lp1, lp1);
        gateToGateSpace = Math.max(getSpacing(lgate, lgate), diffExtendAlongMos);
        selectSpace = getSpacing(lnsel, lnsel);
        DRCTemplate m1MinAreaRule = drcRules.getMinValue(lmets[0], DRCRuleType.MINAREA);
        m1MinArea = m1MinAreaRule != null ? m1MinAreaRule.getValue(0) : 0.0;

        Technology.NodeLayer ndm1_nd = findNodeLayer(ndm1, lnd);
        Technology.NodeLayer ndm1_nsel = findNodeLayer(ndm1, lnsel);
        Technology.NodeLayer ndm1_m1 = findNodeLayer(ndm1, lmets[0]);
        Technology.NodeLayer ndm1_multicut = findMulticut(ndm1);
        diffContWidth = DBMath.gridToLambda(ndm1_nd.getRightEdge().getGridAdder() - ndm1_nd.getLeftEdge().getGridAdder());
        diffCont_m1Width = DBMath.gridToLambda(ndm1_m1.getRightEdge().getGridAdder() - ndm1_m1.getLeftEdge().getGridAdder());
        selectSurroundDiffInActiveContact = DBMath.gridToLambda(ndm1_nsel.getRightEdge().getGridAdder() - ndm1_nd.getRightEdge().getGridAdder());
        diffContIncr = DBMath.gridToLambda(ndm1_multicut.getGridMulticutSizeX() + ndm1_multicut.getGridMulticutSep1D());
        if (drcRules.isAnySpacingRule(ndm1_multicut.getLayer(), lgate)) {
            gateToDiffContSpace = gateToDiffContSpaceDogBone = DBMath.round(getSpacing(ndm1_multicut.getLayer(), lgate)
                - 0.5*(diffContWidth - ndm1_multicut.getMulticutSizeX()));
        } else {
            gateToDiffContSpace = gateToDiffContSpaceDogBone = 0;
        }

        Technology.NodeLayer p1m1_p1 = findNodeLayer(p1m1, lp1);
        polyContWidth = DBMath.gridToLambda(p1m1_p1.getRightEdge().getGridAdder() - p1m1_p1.getLeftEdge().getGridAdder());
        offsetLShapePolyContact = DBMath.gridToLambda(p1m1_p1.getRightEdge().getGridAdder() - p1.getLayerGridExtend(lp1));
        offsetTShapePolyContact = DBMath.gridToLambda(p1m1_p1.getRightEdge().getGridAdder() + p1.getLayerGridExtend(lp1));

        selectSurround = Double.NaN;
    }

    //---------------------------- public classes -----------------------------
    /** Hide the differences between technologies. A MosInst's gate is always
     * vertical. */
    public static class MosInst {
        private final NodeInst mos;
        private final String leftDiff, rightDiff, topPoly, botPoly;
        private static void error(boolean pred, String msg) {
            Job.error(pred, msg);
        }
        private MosInst(char np, double x, double y, double width, double length,
                        TechType tech, Cell parent) {
            Transistor t = np=='n' ? tech.nmos : tech.pmos;
            this.leftDiff = t.leftDiff.getName();
            this.rightDiff = t.rightDiff.getName();
            this.topPoly = t.topPoly.getName();
            this.botPoly = t.bottomPoly.getName();
            if (tech.rotateTransistors) {
                mos = LayoutLib.newNodeInst(t.pn, x, y, width, length, 90, parent);
            } else {
                mos = LayoutLib.newNodeInst(t.pn, x, y, length, width, 0, parent);
            }
        }
        private PortInst getPort(String portNm) {
            PortInst pi = mos.findPortInst(portNm);
            error(pi==null, "MosInst can't find port!");
            return pi;
        }
        public PortInst leftDiff() {return getPort(leftDiff);}
        public PortInst rightDiff() {return getPort(rightDiff);}
        public PortInst topPoly() {return getPort(topPoly);}
        public PortInst botPoly() {return getPort(botPoly);}
    }

    //------------------------------ public data ------------------------------

    public static TechType getTechType(Technology technology) {
        if (technology == Technology.getMocmosTechnology()) {
            return getMOCMOS();
        } else if (technology == Technology.getTSMC180Technology()) {
            return getTSMC180();
        } else if (technology == Technology.getCMOS90Technology()) {
            return getCMOS90();
        } else {
            return new TechTypeWizard(technology);
//            System.out.println("Invalid TechTypeEnum");
//            throw new AssertionError();
        }
    }

    private static TechType techTypeMoCMOS;

    public static TechType getMOCMOS() {
        if (techTypeMoCMOS == null) {
            techTypeMoCMOS = new TechTypeMoCMOS();
        }
        return techTypeMoCMOS;
    }

    private static TechType techTypeTSMC180;

    public static TechType getTSMC180() {
        if (techTypeTSMC180 == null) {
            try {
                Class tsmc180Class = Class.forName("com.sun.electric.plugins.tsmc.TechTypeTSMC180");
                Constructor<TechType> techConstr = tsmc180Class.getConstructor();
                techTypeTSMC180 = techConstr.newInstance();
            } catch (Exception e) {
                assert (false); // runtime error
            }
        }
        return techTypeTSMC180;
    }

    private static TechType techTypeCMOS90;

    public static TechType getCMOS90() {
        if (techTypeCMOS90 == null) {
            try {
                Class cmos90Class = Class.forName("com.sun.electric.plugins.tsmc.TechTypeCMOS90");
                Constructor<TechType> techConstr = cmos90Class.getConstructor();
                techTypeCMOS90 = techConstr.newInstance();
    //			java.lang.reflect.Field techField = cmos90Class.getDeclaredField("CMOS90");
    //			cmos90 = (TechType) techField.get(null);
            } catch (Exception e) {
                assert (false); // runtime error
            }
        }
        return techTypeCMOS90;
    }

    //----------------------------- public methods ----------------------------

    public int getNumMetals() { return lmets.length; }
    public Technology getTechnology() {return technology;}

    /** layers */
    public ArcProto pdiff() {return pdiff;}
    public ArcProto ndiff() {return ndiff;}
    public ArcProto p1() {return p1;}
    public ArcProto m1() {return m1;}
    public ArcProto m2() {return m2;}
    public ArcProto m3() {return m3;}
    public ArcProto m4() {return m4;}
    public ArcProto m5() {return m5;}
    public ArcProto m6() {return m6;}
    public ArcProto m7() {return m7;}
    public ArcProto m8() {return m8;}
    public ArcProto m9() {return m9;}
    public ArcProto ndiff18() {return ndiff18;}
    public ArcProto pdiff18() {return pdiff18;}
    public ArcProto ndiff25() {return ndiff25;}
    public ArcProto pdiff25() {return pdiff25;}
    public ArcProto ndiff33() {return ndiff33;}
    public ArcProto pdiff33() {return pdiff33;}

    /** pins */
    public PrimitiveNode ndpin() {return ndpin;}
    public PrimitiveNode pdpin() {return pdpin;}
    public PrimitiveNode p1pin() {return p1pin;}
    public PrimitiveNode m1pin() {return m1pin;}
    public PrimitiveNode m2pin() {return m2pin;}
    public PrimitiveNode m3pin() {return m3pin;}
    public PrimitiveNode m4pin() {return m4pin;}
    public PrimitiveNode m5pin() {return m5pin;}
    public PrimitiveNode m6pin() {return m6pin;}
    public PrimitiveNode m7pin() {return m7pin;}
    public PrimitiveNode m8pin() {return m8pin;}
    public PrimitiveNode m9pin() {return m9pin;}

    /** vias */
    public PrimitiveNode nwm1() {return nwm1;}
    public PrimitiveNode pwm1() {return pwm1;}
    public PrimitiveNode nwm1Y() {return nwm1Y;}
    public PrimitiveNode pwm1Y() {return pwm1Y;}
    public PrimitiveNode ndm1() {return ndm1;}
    public PrimitiveNode pdm1() {return pdm1;}
    public PrimitiveNode p1m1() {return p1m1;}
    public PrimitiveNode m1m2() {return m1m2;}
    public PrimitiveNode m2m3() {return m2m3;}
    public PrimitiveNode m3m4() {return m3m4;}
    public PrimitiveNode m4m5() {return m4m5;}
    public PrimitiveNode m5m6() {return m5m6;}
    public PrimitiveNode m6m7() {return m6m7;}
    public PrimitiveNode m7m8() {return m7m8;}
    public PrimitiveNode m8m9() {return m8m9;}

    /** Transistors */
    public PrimitiveNode nmos() {return nmos != null ? nmos.pn : null;}
    public PrimitiveNode pmos() {return pmos != null ? pmos.pn : null;}
    public PrimitiveNode nmos18() {return nmos18 != null ? nmos18.pn : null;}
    public PrimitiveNode pmos18() {return pmos18 != null ? pmos18.pn : null;}
    public PrimitiveNode nmos25() {return nmos25 != null ? nmos25.pn : null;}
    public PrimitiveNode pmos25() {return pmos25 != null ? pmos25.pn : null;}
    public PrimitiveNode nmos33() {return nmos33 != null ? nmos33.pn : null;}
    public PrimitiveNode pmos33() {return pmos33 != null ? pmos33.pn : null;}

    /** special threshold transistor contacts */
    public PrimitiveNode nmos18contact() {return nmos18contact;}
    public PrimitiveNode pmos18contact() {return pmos18contact;}
    public PrimitiveNode nmos25contact() {return nmos25contact;}
    public PrimitiveNode pmos25contact() {return pmos25contact;}
    public PrimitiveNode nmos33contact() {return nmos33contact;}
    public PrimitiveNode pmos33contact() {return pmos33contact;}

    /** Well */
    public PrimitiveNode nwell() {return nwellNode;}
    public PrimitiveNode pwell() {return pwellNode;}

    /** Layer nodes are sometimes used to patch notches */
    public PrimitiveNode m1Node() {return m1Node;}
    public PrimitiveNode m2Node() {return m2Node;}
    public PrimitiveNode m3Node() {return m3Node;}
    public PrimitiveNode m4Node() {return m4Node;}
    public PrimitiveNode m5Node() {return m5Node;}
    public PrimitiveNode m6Node() {return m6Node;}
    public PrimitiveNode m7Node() {return m7Node;}
    public PrimitiveNode m8Node() {return m8Node;}
    public PrimitiveNode m9Node() {return m9Node;}
    public PrimitiveNode p1Node() {return p1Node;}
    public PrimitiveNode pdNode() {return pdNode;}
    public PrimitiveNode ndNode() {return ndNode;}
    public PrimitiveNode pselNode() {return pselNode;}
    public PrimitiveNode nselNode() {return nselNode;}

    /** Transistor layer nodes */
    public PrimitiveNode od18() {return od18Node;}
    public PrimitiveNode od25() {return od25Node;}
    public PrimitiveNode od33() {return od33Node;}
    public PrimitiveNode vth() {return vthNode;}
    public PrimitiveNode vtl() {return vtlNode;}

    /** Essential-Bounds */
    public PrimitiveNode essentialBounds() {return essentialBounds;}

    /** Facet-Center */
    public PrimitiveNode facetCenter() {return facetCenter;}

    public PrimitiveNode getViaFor(ArcProto a1, ArcProto a2) {
        return viaMap.get(new ArcPair(a1, a2));
    }

    public int layerHeight(ArcProto p) {
        for (int i = 0; i < nbLay; i++) {
            if (arcs[i] == p)
                return i;
        }
        error(true, "Can't find layer: " + p);
        return -1;
    }

    public ArcProto closestLayer(PortProto port, ArcProto layer) {
        int h = layerHeight(layer);
        for (int dist = 0; dist < nbLay; dist++) {
            int lookUp = h + dist;
            int lookDn = h - dist;
            if (lookUp < nbLay) {
                ArcProto lay = layerAtHeight(lookUp);
                if (port.connectsTo(lay))
                    return lay;
            }
            if (lookDn >= 0) {
                ArcProto lay = layerAtHeight(lookDn);
                if (port.connectsTo(lay))
                    return lay;
            }
        }
        error(true, "port can't connect to any layer?!!");
        return null;
    }
    
    public ArcProto highestLayer(PortProto port) {
    	for (int h=arcs.length-1; h>=0; h--) {
    		if (port.connectsTo(arcs[h])) return arcs[h];
    	}
    	error(true, "port can't connect to any layer?!!");
    	return null;
    }
    
    public ArcProto layerAtHeight(int layHeight) {return arcs[layHeight];	}

    public PrimitiveNode viaAbove(int layHeight) {return vias[layHeight];}

    public PrimitiveNode viaBelow(int layHeight) {return vias[layHeight - 1];}

    /** round to avoid MOCMOS CIF resolution errors */
    public double roundToGrid(double x) {
        return x;
    }

	public MosInst newNmosInst(double x, double y,
							   double w, double l, Cell parent) {
		return new MosInst('n', x, y, w, l, this, parent);
	}
	public MosInst newPmosInst(double x, double y,
							   double w, double l, Cell parent) {
		return new MosInst('p', x, y, w, l, this, parent);
	}
    public abstract String name();

    public abstract double reservedToLambda(int layer, double nbTracks);
    
	/** @return min width of Well */
	public double getWellWidth() {return nwm1.getMinSizeRule().getWidth();}
	/** @return amount that well surrounds diffusion */
	public double getWellSurroundDiffInWellContact() {return wellSurroundDiff;}
	/** @return MOS edge to gate edge */
    public double getGateExtendPastMOS() {return gateExtendPastMOS;}
    /** @return min width of polysilicon 1 */
    public double getP1Width() {return p1Width;}
    /** @return min spacing between polysilicon 1 */
    public double getP1ToP1Space() {return p1ToP1Space;}
    /** @return min spacing between gates of series transistors */ 
    public double getGateToGateSpace() {return gateToGateSpace;}
    /** @return min spacing between MOS gate and diffusion edge of diff contact */
    public double getGateToDiffContSpace() {return gateToDiffContSpace;}
    /** @return min spacing between MOS gate and diffusion edge of diff contact
     * when the diffusion width is larger than the gate width */
    public double getGateToDiffContSpaceDogBone() {return gateToDiffContSpaceDogBone;}
    /** @return min width of diffusion surrounding well contact */
    public double getWellContWidth() { return wellContWidth; }
    /** @return min width of diffusion surrounding diff contact */
    public double getDiffContWidth() { return diffContWidth; }
//        SizeOffset so = ndm1().getProtoSizeOffset();
//        return (ndm1().getMinSizeRule().getHeight() - so.getHighYOffset() - so.getLowYOffset());
//    }
    /** @return min width of poly contact */
    public double getP1M1Width() { return polyContWidth; }
//        SizeOffset so = p1m1().getProtoSizeOffset();
//        return (p1m1().getMinSizeRule().getHeight() - so.getHighYOffset() - so.getLowYOffset());
//    }
    /** @return gate length that depends on foundry */
    public double getGateLength() {return gateLength;}
    /** @return amount that select surrounds diffusion in well? */
    public double selectSurroundDiffInWellContact() {return selectSurroundDiffInWellContact;}
    /** @return amount that select surrounds diffusion in well? */
    public double selectSurroundDiffInDiffContact() {return selectSurroundDiffInActiveContact;}
    /** @return amount that Select surrounds MOS, along gate width dimension */
    public double selectSurroundDiffAlongGateInTrans() {return selectSurroundDiffAlongGateInTrans;}
    /** @return y offset of poly arc connecting poly contact and gate in a L-Shape case */
    public double getPolyLShapeOffset() {return offsetLShapePolyContact;}
    /** @return y offset of poly arc connecting poly contact and gate in a T-Shape case */
    public double getPolyTShapeOffset() {return offsetTShapePolyContact;}
    /** @return select spacing rule */
    public double getSelectSpacingRule() {return selectSpace;}
    /** @return select surround active in transistors but not along the gate */
    public double getSelectSurroundDiffInTrans() {return selectSurroundDiffInTrans;}
    /** @return select surround over poly */
    public double getSelectSurroundOverPoly() {return selectSurround;}
    /** @return minimum metal1 area (sq lambda) */
    public double getM1MinArea() {return m1MinArea;}
    /** @return width of metal-1 in min sized diffusion contact */
    public double getDiffCont_m1Width() {return diffCont_m1Width;}
    /** @return amount diffusion contact grows to accomodate an one additional contact cut */ 
    public double getDiffContIncr() {return diffContIncr;}
    
    public Variable.Key getAttrX() {return ATTR_X;}
    public Variable.Key getAttrS() {return ATTR_S;}
    public Variable.Key getAttrSP() {return ATTR_SP;}
    public Variable.Key getAttrSN() {return ATTR_SN;}


}
