package org.jboss.as.console.client.shared.subsys.jsmpolicy;

import java.util.ArrayList;
import java.util.List;

public class JsmNode {
	String name;
	boolean group;
	String policy;
	List<JsmNode> nodes = new ArrayList<JsmNode>();

	public JsmNode(String name, boolean group){
		this.name = name;
		this.group = group;
	}

	public String getName(){
		return name;
	}

	public List<JsmNode> getNodes(){
		return nodes;
	}

	public boolean isGroup(){
		return group;
	}

	public String getPolicy(){
	    return "policy2";
	}

	public void setPolicy(String policy){

	}

}