/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PinEdge.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.sctiming;

/**
 * Describe a pin and an associated Transition
 * User: gainsley
 * Date: Nov 16, 2006
 */
public class PinEdge {

    /**
     * Describe the transition on a pin
     */
    public enum Transition {
        /** a stable high voltage */                STABLE1,
        /** a stable low voltage */                 STABLE0,
        /** a rising edge */                        RISE,
        /** a falling edge */                       FALL,
        /** a stable analog voltage */              STABLEV }

    String pin;
    Transition transition;
    double stableVoltage;

    /**
     * Create a new pin edge with an associated transition
     * @param pin pin name
     * @param transition transition type
     */
    public PinEdge(String pin, Transition transition) {
        this.pin = pin;
        this.transition = transition;
        stableVoltage = 0;
    }

    /**
     * Create a new pin edge with a stable voltage
     * @param pin pin name
     * @param stableVoltage voltage level
     */
    public PinEdge(String pin, double stableVoltage) {
        this.pin = pin;
        this.transition = Transition.STABLEV;
        this.stableVoltage = stableVoltage;
    }

    /**
     * Get a stable PinEdge representing the final
     * state of this PinEdge, after any transition.
     * @return a PinEdge
     */
    public PinEdge getFinalState() {
        Transition t = transition;
        if (transition == Transition.RISE) t = Transition.STABLE1;
        if (transition == Transition.FALL) t = Transition.STABLE0;
        return new PinEdge(pin, t);
    }

    /**
     * Get a stable PinEdge representing the initial
     * state of this PinEdge, before any transition.
     * @return a PinEdge
     */
    public PinEdge getInitialState() {
        Transition t = transition;
        if (transition == Transition.RISE) t = Transition.STABLE0;
        if (transition == Transition.FALL) t = Transition.STABLE1;
        return new PinEdge(pin, t);
    }

    public PinEdge getOpposite() {
        Transition t = transition;
        if (transition == Transition.RISE) t = Transition.FALL;
        else if (transition == Transition.FALL) t = Transition.RISE;
        else if (transition == Transition.STABLE0) t = Transition.STABLE1;
        else if (transition == Transition.STABLE1) t = Transition.STABLE0;
        else if (transition == Transition.STABLEV) return new PinEdge(pin, stableVoltage);

        return new PinEdge(pin, t);
    }

}
