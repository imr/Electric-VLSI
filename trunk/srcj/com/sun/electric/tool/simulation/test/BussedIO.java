package com.sun.electric.tool.simulation.test;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: May 25, 2007
 * Time: 1:04:19 PM
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */
public interface BussedIO {

    public int getWidth();

    public String getName();

    public String getSignal(int index);

    public String getSignal(String bitname);

}
