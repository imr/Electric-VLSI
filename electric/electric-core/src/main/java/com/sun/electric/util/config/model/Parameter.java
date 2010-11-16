/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Parameter.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.util.config.model;

import java.util.Map;

import com.sun.electric.util.config.SimpleEnumConstructor.TestEnum;

/**
 * @author Felix Schmidt
 */
public class Parameter {

	public enum Attributes {
		name, ref, value, type;
	}

	public enum Type {
		String, Integer, Double, Boolean, Reference, Enum
	}

	private String name;
	private String ref;
	private String value;
	private Type type;

	/**
	 * @param name
	 * @param ref
	 * @param value
	 * @param type
	 */
	public Parameter(String name, String ref, String value, Type type) {
		super();
		this.name = name;
		this.ref = ref;
		this.value = value;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public ParameterEntry createParameter(Map<String, Injection> allInjections) throws ClassNotFoundException {
		ParameterEntry entry = null;

		if (this.ref == null) {
			entry = new ParameterEntry(this.name, createByType());
		} else {
			entry = new ParameterEntry(name, createByReference(allInjections));
		}

		return entry;
	}

	private ConfigEntry<?> createByType() throws ClassNotFoundException {

		if (type.equals(Type.Boolean)) {
			return new ConfigEntry.ConfigEntryPrimitive<Boolean>(Boolean.parseBoolean(value));
		} else if (type.equals(Type.Double)) {
			return new ConfigEntry.ConfigEntryPrimitive<Double>(Double.parseDouble(value));
		} else if (type.equals(Type.Integer)) {
			return new ConfigEntry.ConfigEntryPrimitive<Integer>(Integer.parseInt(value));
		} else if (type.equals(Type.String)) {
			return new ConfigEntry.ConfigEntryPrimitive<String>(value);
		} else if (type.equals(Type.Enum)) {
			return ConfigEntry.createForEnum((Class<Enum>)(Class.forName(value.substring(0, value.lastIndexOf('.')))),
					value.substring(value.lastIndexOf('.') + 1));
			//return new ConfigEntry.ConfigEntryEnum<TestEnum>(TestEnum.class, "EnumValue1");
		}

		return null;
	}

	private ConfigEntry<?> createByReference(Map<String, Injection> allInjections)
			throws ClassNotFoundException {
		ConfigEntry<?> entry = ConfigEntries.getEntries().get(this.ref);

		if (entry != null) {
			return entry;
		} else {
			return allInjections.get(this.ref).createConfigEntry(allInjections);
		}
	}

}
