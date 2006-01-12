package com.sun.electric.technology;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Jan 11, 2006
 * Time: 10:05:40 AM
 * To change this template use File | Settings | File Templates.
 */
/**
 * This is supposed to better encapsulate a particular foundry
 * associated to a technology plus the valid DRC rules.
 */
public class Foundry {

    public enum Type {
        /** None */                                                         NONE (-1),
        /** only for TSMC technology */                                     TSMC (010000),
        /** only for ST technology */                                       ST (020000),
        /** only for MOSIS technology */                                    MOSIS (040000);
        private final int mode;
        Type(int mode) {
            this.mode = mode;
        }
        public int mode() { return this.mode; }
        public String toString() {return name();}
    }
    private Type type;
    private List<DRCTemplate> rules;
    public Foundry(Type mode) {
        this.type = mode;
    }
    public Type getType() { return type; }
    public List<DRCTemplate> getRules() { return rules; }
    public void setRules(List<DRCTemplate> list) { rules = list; }
}
