package com.sun.electric.tool.simulation.eventsim.core.classRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


public class ComponentInfo {

	
	public Class componentType;
	public String className;
	public String componentTypeName;
	public LinkedList<String> tags;
	public LinkedList<String> inputs;
	public Map<String, LinkedList<String>> virtualInputs;
	public LinkedList<String> outputs;
	public Map<String, LinkedList<String>> virtualOutputs;
	public LinkedHashMap<String, String> parameters;
	public LinkedHashMap<String, String> globals;
	
	public ComponentInfo() {
		componentType= null;
		componentTypeName= null;
		tags= new LinkedList<String>();
		inputs= new LinkedList<String>();
		virtualInputs= new HashMap<String, LinkedList<String>>();
		outputs= new LinkedList<String>();		
		virtualOutputs= new HashMap<String, LinkedList<String>>();
		parameters= new LinkedHashMap<String, String>();
		globals= new LinkedHashMap<String, String>();
	}
	
	public void setClass(Class c) {
		componentType= c;
	}
	
	public void setName(String n) {
		componentTypeName= n;
	}
	
	public void addTag(String tag) {
		tags.add(tag);
	}
	
	public void addInput(String input) {
		inputs.add(input);
		LinkedList<String> vl= new LinkedList<String>();
		virtualInputs.put(input, vl);
	}
	
	public void addVirtualInput(String input, String vInput) {
		LinkedList<String> vl= virtualInputs.get(input);
		vl.addLast(vInput);
	}

	public void addOutput(String output) {
		outputs.add(output);
		LinkedList<String> vl= new LinkedList<String>();
		virtualOutputs.put(output, vl);
	}
	
	public void addVirtualOutput(String output, String vOutput) {
		LinkedList<String> vl= virtualInputs.get(output);
		vl.addLast(vOutput);
	}

	public void addParameter(String name, String type) {
		parameters.put(name, type);
	}
	
	public void addGlobal(String name, String type) {
		globals.put(name, type);
	}
	
	public Class getComponentType() {
		return componentType;
	}
	
	public String getName() {
		return componentTypeName;
	}

	public Collection<String> getTags() {
		return tags;
	}
	
	public Collection<String> getInputs() {
		return inputs;
	}
	
	public Collection<String> getVirtualInputs(String in) {
		LinkedList<String> vpList= virtualInputs.get(in);
		if (vpList == null) {
			return new LinkedList<String>();
		}
		else {
			return vpList;
		}
	}
	
	public Collection<String> getVirtualOutputs(String out) {
		LinkedList<String> vpList= virtualOutputs.get(out);
		if (vpList == null) {
			return new LinkedList<String>();
		}
		else {
			return vpList;
		}
	}
	
	public Collection<String> getOutputs() {
		return outputs;
	}
	
	public Collection<String> getParameters() {
		return parameters.keySet();
	}
	
	public String getParameterType(String parameter) {
		return parameters.get(parameter);
	}
	
	public Collection<String> getGlobals() {
		return globals.keySet();
	}
	
	public String getGlobalType(String global) {
		return globals.get(global);
	}
	
	public String toString() {
		String result= "Component info: \n";
		result+= "\tName: " + componentTypeName + "\n";
		result+= "\tClass: " + componentType+ " = " + className + "\n";
		
		String tagString="\tTags: [ ";
		for (String tag: tags) {
			tagString+= " "+ tag + " ";
		}
		tagString+= " ]";
		result+= tagString + "\n";

		String inputString="\tInputs: [ ";
		for (String input: inputs) {
			inputString+= " "+ input + " ";
			LinkedList<String> vi= virtualInputs.get(input);
			if (vi.size() > 0) {
				inputString+= "[";
				for (String vInput : vi) {
					inputString+= " " + vInput + " "; 
				}
				inputString+= "] ";
			}
		}
		inputString+= " ]";
		result+= inputString + "\n";

		String outputString="\tOutputs: [ ";
		for (String output: outputs) {
			outputString+= " "+ output + " ";
			LinkedList<String> vo= virtualOutputs.get(output);
			if (vo.size() > 0) {
				outputString+= "[";
				for (String vOutput : vo) {
					outputString+= " " + vOutput + " "; 
				}
				outputString+= "] ";
			}
		}
		outputString+= " ]";
		result+= outputString + "\n";

		String parameterString= "\tParameters: [ ";
		for (Entry e : parameters.entrySet()) {
			parameterString+= " ( " + e.getKey() + ", " + e.getValue() + " )";
		}
		parameterString+= " ]";
		result+= parameterString + "\n";
		
		String globalString= "\tGlobals: [ ";
		for (Entry e : globals.entrySet()) {
			globalString+= " ( " + e.getKey() + ", " + e.getValue() + " )";
		}
		globalString+= " ]";
		result+= globalString;
		
		return result;
	}
	
} // class ComponentInfo
