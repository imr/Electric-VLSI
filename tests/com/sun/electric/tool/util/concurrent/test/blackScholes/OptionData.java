/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OptionData.java
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
package com.sun.electric.tool.util.concurrent.test.blackScholes;

/**
 * @author Felix Schmidt
 * 
 */
public class OptionData {

	public enum OptionType {
		put, call;
	}

	private double spot;
	private double strike;
	private double riskFree;
	private double divq;
	private double volatility;
	private double ttm;
	private double price;

	/**
	 * @return the spot
	 */
	public double getSpot() {
		return spot;
	}

	/**
	 * @param spot
	 *            the spot to set
	 */
	public void setSpot(double spot) {
		this.spot = spot;
	}

	/**
	 * @return the strike
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * @param strike
	 *            the strike to set
	 */
	public void setStrike(double strike) {
		this.strike = strike;
	}

	/**
	 * @return the riskFree
	 */
	public double getRiskFree() {
		return riskFree;
	}

	/**
	 * @param riskFree
	 *            the riskFree to set
	 */
	public void setRiskFree(double riskFree) {
		this.riskFree = riskFree;
	}

	/**
	 * @return the divq
	 */
	public double getDivq() {
		return divq;
	}

	/**
	 * @param divq
	 *            the divq to set
	 */
	public void setDivq(double divq) {
		this.divq = divq;
	}

	/**
	 * @return the volatility
	 */
	public double getVolatility() {
		return volatility;
	}

	/**
	 * @param volatility
	 *            the volatility to set
	 */
	public void setVolatility(double volatility) {
		this.volatility = volatility;
	}

	/**
	 * @return the ttm
	 */
	public double getTtm() {
		return ttm;
	}

	/**
	 * @param ttm
	 *            the ttm to set
	 */
	public void setTtm(double ttm) {
		this.ttm = ttm;
	}

	/**
	 * @return the type
	 */
	public OptionType getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(OptionType type) {
		this.type = type;
	}

	/**
	 * @return the divs
	 */
	public double getDivs() {
		return divs;
	}

	/**
	 * @param divs
	 *            the divs to set
	 */
	public void setDivs(double divs) {
		this.divs = divs;
	}

	/**
	 * @return the refValue
	 */
	public double getRefValue() {
		return refValue;
	}

	/**
	 * @param refValue
	 *            the refValue to set
	 */
	public void setRefValue(double refValue) {
		this.refValue = refValue;
	}

	private OptionType type;
	private double divs;
	private double refValue;

	/**
	 * @param spot
	 * @param strike
	 * @param riskFree
	 * @param divq
	 * @param volatility
	 * @param ttm
	 * @param type
	 * @param divs
	 * @param refValue
	 */
	public OptionData(double spot, double strike, double riskFree, double divq, double volatility,
			double ttm, OptionType type, double divs, double refValue) {
		super();
		this.spot = spot;
		this.strike = strike;
		this.riskFree = riskFree;
		this.divq = divq;
		this.volatility = volatility;
		this.ttm = ttm;
		this.type = type;
		this.divs = divs;
		this.refValue = refValue;
	}
	
	public OptionData() {
		
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(double price) {
		this.price = price;
	}

	/**
	 * @return the price
	 */
	public double getPrice() {
		return price;
	}

}
