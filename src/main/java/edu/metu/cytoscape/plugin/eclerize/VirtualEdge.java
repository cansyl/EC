package edu.metu.cytoscape.plugin.eclerize;

import org.cytoscape.view.layout.LayoutNode;

public class VirtualEdge {
	
	public LayoutNode to;
	public LayoutNode from;
	
	public double multiplier;
	
	public VirtualEdge(LayoutNode to, LayoutNode from, double multiplier) {
		this.to = to;
		this.from = from;
		this.multiplier = multiplier;
	}
	
}
