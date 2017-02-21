package edu.metu.cytoscape.plugin.eclerize;

public class Point {
	
	public double x;
	public double y;
	
	public Point(double x,double y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object obj) {
		Point p2 = (Point) obj;
		if(this.x == p2.x && this.y == p2.y) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return "x: " + x + " y: " + y;
	}
}
