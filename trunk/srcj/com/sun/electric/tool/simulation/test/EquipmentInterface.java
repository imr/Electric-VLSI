package com.sun.electric.tool.simulation.test;

/**
 * Equipment implements only a GPIB equipment.
 * <p>
 * EquipmentInterface abstracts Equipment so that non-GPIB (e.g. socket) based
 * equipment can be configured.
 *
 * @author Frankie Liu
 * @version 1.0 January 3, 2005
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */

public interface EquipmentInterface {
    public void write(String data);
    public String read(int length);
    public String readLine();
}
