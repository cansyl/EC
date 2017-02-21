package edu.metu.cytoscape.plugin.eclerize;

import java.awt.Color;

public enum ColorCode {

	NONE(Color.WHITE),
	FIRST(Color.CYAN),
	SECOND(Color.MAGENTA),
	THIRD(Color.GREEN),
	FOURTH(Color.BLUE),
	FIFTH(Color.ORANGE),
	SIXTH(Color.PINK);
	
	private final Color color;
	
	ColorCode(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}
	
	public static ColorCode getCode(String ec) {
		if(ec==null || ec.length()<1) {
			return NONE;
		}
		
		if(ec.startsWith("1")) {
			return FIRST;
		}else if(ec.startsWith("2")) {
			return SECOND;
		}else if(ec.startsWith("3")) {
			return THIRD;
		}else if(ec.startsWith("4")) {
			return FOURTH;
		}else if(ec.startsWith("5")) {
			return FIFTH;
		}else if(ec.startsWith("6")) {
			return SIXTH;
		}
		
		return NONE;
	}
}
